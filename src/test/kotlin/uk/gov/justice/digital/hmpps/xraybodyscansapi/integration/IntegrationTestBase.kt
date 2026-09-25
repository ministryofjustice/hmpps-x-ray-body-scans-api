package uk.gov.justice.digital.hmpps.xraybodyscansapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.reactive.server.WebTestClient
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.xraybodyscansapi.config.ADMIN_ROLE
import uk.gov.justice.digital.hmpps.xraybodyscansapi.config.READ_CASE_NOTE_ROLE
import uk.gov.justice.digital.hmpps.xraybodyscansapi.config.READ_ROLE
import uk.gov.justice.digital.hmpps.xraybodyscansapi.config.WRITE_CASE_NOTE_ROLE
import uk.gov.justice.digital.hmpps.xraybodyscansapi.config.WRITE_ROLE
import uk.gov.justice.digital.hmpps.xraybodyscansapi.event.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.testcontainers.LocalStackContainer
import uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.testcontainers.LocalStackContainer.setLocalStackProperties
import uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.wiremock.HmppsAuthApiExtension
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException
import uk.gov.justice.hmpps.sqs.MissingTopicException
import uk.gov.justice.hmpps.sqs.countAllMessagesOnQueue
import uk.gov.justice.hmpps.sqs.publish
import uk.gov.justice.hmpps.test.kotlin.auth.JwtAuthorisationHelper

@ExtendWith(HmppsAuthApiExtension::class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureWebTestClient
abstract class IntegrationTestBase {
  @Autowired
  protected lateinit var webTestClient: WebTestClient

  @Autowired
  protected lateinit var jwtAuthHelper: JwtAuthorisationHelper

  @Autowired
  protected lateinit var jsonMapper: JsonMapper

  @MockitoSpyBean
  protected lateinit var hmppsQueueService: HmppsQueueService

  @BeforeEach
  fun `clear queues`() {
    hmppsDomainEventsQueue.sqsClient.purgeQueue(
      PurgeQueueRequest.builder().queueUrl(hmppsDomainEventsQueue.queueUrl).build(),
    ).get()
  }

  protected val domainEventsTopic by lazy {
    hmppsQueueService.findByTopicId("hmppseventtopic")
      ?: throw MissingTopicException("hmppseventtopic not found")
  }

  protected val hmppsDomainEventsQueue by lazy {
    hmppsQueueService.findByQueueId("hmppsdomaineventsqueue")
      ?: throw MissingQueueException("hmppsdomaineventsqueue queue not found")
  }

  protected fun sendDomainEvent(event: HmppsDomainEvent) {
    domainEventsTopic.publish(event.eventType, jsonMapper.writeValueAsString(event))
  }

  protected fun HmppsQueue.countAllMessagesOnQueue(): Int = sqsClient.countAllMessagesOnQueue(queueUrl).get()

  protected fun setAuthorisation(
    username: String? = "AUTH_ADM",
    roles: List<String> = listOf(),
    scopes: List<String> = listOf("read"),
  ): (HttpHeaders) -> Unit = jwtAuthHelper.setAuthorisationHeader(username = username, scope = scopes, roles = roles)

  protected fun endpointIsProtected(
    /** This request should be successful given a properly authorised token (valid url, method and payload) */
    request: WebTestClient.RequestHeadersSpec<*>,
    authorisedRoles: Set<String>,
    setupSuccess: (() -> Unit)? = null,
    verifyFailure: (() -> Unit)? = null,
  ): List<DynamicTest> = buildList {
    assertThat(authorisedRoles).isNotEmpty()
    val request = request.header("Content-Type", "application/json")

    add(
      DynamicTest.dynamicTest("returns 401 given no authority") {
        request
          .exchange()
          .expectStatus().isUnauthorized
        verifyFailure?.invoke()
      },
    )

    add(
      DynamicTest.dynamicTest("returns 403 given no roles") {
        request
          .headers(setAuthorisation())
          .exchange()
          .expectStatus().isForbidden
        verifyFailure?.invoke()
      },
    )

    add(
      DynamicTest.dynamicTest("returns 403 given wrong role") {
        request
          .headers(setAuthorisation(roles = listOf("ROLE_PRISONER_SEARCH")))
          .exchange()
          .expectStatus().isForbidden
        verifyFailure?.invoke()
      },
    )

    setOf(
      READ_ROLE,
      WRITE_ROLE,
      READ_CASE_NOTE_ROLE,
      WRITE_CASE_NOTE_ROLE,
      ADMIN_ROLE,
    ).subtract(authorisedRoles).forEach { unauthorisedRole ->
      add(
        DynamicTest.dynamicTest("returns 403 given insufficiently capable role $unauthorisedRole") {
          request
            .headers(setAuthorisation(roles = listOf(unauthorisedRole)))
            .exchange()
            .expectStatus().isForbidden
          verifyFailure?.invoke()
        },
      )
    }

    authorisedRoles.forEach { authorisedRole ->
      add(
        DynamicTest.dynamicTest("permits role $authorisedRole") {
          setupSuccess?.invoke()
          request
            .headers(setAuthorisation(roles = listOf(authorisedRole)))
            .exchange()
            .expectStatus().is2xxSuccessful
        },
      )
    }
  }

  fun WebTestClient.ResponseSpec.expectErrorResponse(
    status: HttpStatus = HttpStatus.BAD_REQUEST,
    userMessageContains: String,
    developerMessageContains: String,
  ) {
    expectStatus().isEqualTo(status)
    expectBody()
      .jsonPath("status").isEqualTo(status.value())
      .jsonPath("errorCode").isEqualTo(null)
      .jsonPath("moreInfo").isEqualTo(null)
      .jsonPath("userMessage").value<String> {
        assertThat(it).contains(userMessageContains)
      }
      .jsonPath("developerMessage").value<String> {
        assertThat(it).contains(developerMessageContains)
      }
  }

  companion object {
    private val localStackContainer = LocalStackContainer.instance

    @JvmStatic
    @DynamicPropertySource
    fun properties(registry: DynamicPropertyRegistry) {
      System.setProperty("aws.region", "eu-west-2")
      localStackContainer?.also { setLocalStackProperties(it, registry) }
    }
  }
}
