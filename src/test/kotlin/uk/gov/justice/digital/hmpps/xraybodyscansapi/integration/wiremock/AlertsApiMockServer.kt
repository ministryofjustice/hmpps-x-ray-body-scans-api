package uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.absent
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.havingExactly
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class AlertsApiExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {
  companion object {
    @JvmField
    val alertsApi = AlertsApiMockServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    alertsApi.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    alertsApi.resetRequests()
  }

  override fun afterAll(context: ExtensionContext) {
    alertsApi.stop()
  }
}

class AlertsApiMockServer : WireMockServer(WIREMOCK_PORT) {
  companion object {
    private const val WIREMOCK_PORT = 8092
  }

  fun stubHealthPing(status: Int = 200): StubMapping = stubFor(
    get("/health/ping").willReturn(
      aResponse()
        .withHeader("Content-Type", "application/json")
        // language=json
        .withBody(if (status == 200) """{"status":"UP"}""" else """{"status":"DOWN"}""")
        .withStatus(status),
    ),
  )

  fun stubBulkGetAlerts(
    body: String,
    filterAlertCodes: Set<String>? = null,
    username: String? = null,
  ): StubMapping = stubFor(
    post(urlPathEqualTo("/search/alerts/prison-numbers"))
      .also {
        if (filterAlertCodes != null) {
          it.withQueryParam("filterAlertCodes", havingExactly(*filterAlertCodes.toTypedArray()))
        }
        if (username != null) {
          it.withHeader("Username", equalTo(username))
        } else {
          it.withHeader("Username", absent())
        }
      }
      .willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withStatus(200)
          .withBody(body),
      ),
  )

  fun stubBulkGetAlertsError(status: Int): StubMapping = stubFor(
    post(urlPathEqualTo("/search/alerts/prison-numbers"))
      .willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withStatus(status)
          .withBody(
            // language=json
            """
              {
                "status": $status,
                "errorCode": null,
                "userMessage": "Entity Not Found",
                "developerMessage": "Serious error in the system",
                "moreInfo": null
              }
            """,
          ),
      ),
  )
}
