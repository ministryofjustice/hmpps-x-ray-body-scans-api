package uk.gov.justice.digital.hmpps.xraybodyscansapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.xraybodyscansapi.config.ADMIN_ROLE
import uk.gov.justice.digital.hmpps.xraybodyscansapi.config.READ_CASE_NOTE_ROLE
import uk.gov.justice.digital.hmpps.xraybodyscansapi.config.READ_ROLE
import uk.gov.justice.digital.hmpps.xraybodyscansapi.config.WRITE_CASE_NOTE_ROLE
import uk.gov.justice.digital.hmpps.xraybodyscansapi.config.WRITE_ROLE
import uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.wiremock.HmppsAuthApiExtension
import uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.wiremock.HmppsAuthApiExtension.Companion.hmppsAuth
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

  internal fun setAuthorisation(
    username: String? = "AUTH_ADM",
    roles: List<String> = listOf(),
    scopes: List<String> = listOf("read"),
  ): (HttpHeaders) -> Unit = jwtAuthHelper.setAuthorisationHeader(username = username, scope = scopes, roles = roles)

  protected fun stubPingWithResponse(status: Int) {
    hmppsAuth.stubHealthPing(status)
  }

  protected fun endpointIsProtected(
    /** This request should be successful given a properly authorised token (valid url and payload) */
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
}
