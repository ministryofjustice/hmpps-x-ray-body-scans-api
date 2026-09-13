package uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo

class PrisonApiExtension : MockServerExtension(prisonApi) {
  companion object {
    @JvmField
    val prisonApi = PrisonApiMockServer()
  }
}

class PrisonApiMockServer : WireMockServer(8091) {
  fun stubHealthPing(status: Int = 200) {
    stubFor(
      get("/health/ping").willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          // language=json
          .withBody(if (status == 200) """{"status":"UP"}""" else """{"status":"DOWN"}""")
          .withStatus(status),
      ),
    )
  }

  fun stubGetScanCareNeeds(body: String) {
    stubFor(
      post(urlPathEqualTo("/api/bookings/offenderNo/personal-care-needs"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(body),
        ),
    )
  }

  fun stubGetScanCareNeedsError(status: Int) {
    stubFor(
      post(urlPathEqualTo("/api/bookings/offenderNo/personal-care-needs"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(status)
            .withBody(
              // language=json
              """
              {
                "status": $status,
                "errorCode": 20002,
                "userMessage": "Entity Not Found",
                "developerMessage": "Serious error in the system",
                "moreInfo": "Check out this FAQ for more information"
              }
              """,
            ),
        ),
    )
  }
}
