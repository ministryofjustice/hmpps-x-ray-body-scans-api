package uk.gov.justice.digital.hmpps.xraybodyscansapi.integration

import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.xraybodyscansapi.config.ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RO
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
    /** This request should be successful given properly authorised token (valid url and payload) */
    request: WebTestClient.RequestHeadersSpec<*>,
    requiresWriteRole: Boolean = false,
    afterEach: (() -> Unit)? = null,
  ): List<DynamicTest> = buildList {
    val request = request.header("Content-Type", "application/json")

    add(
      DynamicTest.dynamicTest("returns 401 given no authority") {
        request
          .header(HttpHeaders.AUTHORIZATION)
          .exchange()
          .expectStatus().isUnauthorized
        afterEach?.invoke()
      },
    )

    add(
      DynamicTest.dynamicTest("returns 403 given no roles") {
        request
          .headers(setAuthorisation())
          .exchange()
          .expectStatus().isForbidden
        afterEach?.invoke()
      },
    )

    if (requiresWriteRole) {
      add(
        DynamicTest.dynamicTest("returns 403 given no write role") {
          request
            .headers(setAuthorisation(roles = listOf(ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RO, "ROLE_PRISONER_SEARCH")))
            .exchange()
            .expectStatus().isForbidden
          afterEach?.invoke()
        },
      )
    } else {
      add(
        DynamicTest.dynamicTest("returns 403 given no read role") {
          request
            .headers(setAuthorisation(roles = listOf("ROLE_PRISONER_SEARCH")))
            .exchange()
            .expectStatus().isForbidden
          afterEach?.invoke()
        },
      )
    }
  }
}
