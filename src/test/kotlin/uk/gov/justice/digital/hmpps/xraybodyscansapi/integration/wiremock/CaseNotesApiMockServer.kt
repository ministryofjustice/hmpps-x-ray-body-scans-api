package uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.stubbing.StubMapping

class CaseNotesApiExtension : MockServerExtension(caseNotesApi) {
  companion object {
    @JvmField
    val caseNotesApi = CaseNotesApiMockServer()
  }
}

class CaseNotesApiMockServer : WireMockServer(8093) {
  fun stubHealthPing(status: Int = 200): StubMapping = stubFor(
    get("/health/ping").willReturn(
      aResponse()
        .withHeader("Content-Type", "application/json")
        // language=json
        .withBody(if (status == 200) """{"status":"UP"}""" else """{"status":"DOWN"}""")
        .withStatus(status),
    ),
  )

  fun stubCreateCaseNote(prisonerNumber: String, response: String): StubMapping = stubFor(
    post(urlPathEqualTo("/case-notes/$prisonerNumber"))
      .willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withStatus(200)
          .withBody(response),
      ),
  )

  fun stubCreateCaseNote(prisonerNumber: String, status: Int = 500): StubMapping = stubFor(
    post(urlPathEqualTo("/case-notes/$prisonerNumber"))
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
              "userMessage": "Internal Server Error",
              "developerMessage": "No database connection",
              "moreInfo": null
            }
            """,
          ),
      ),
  )

  fun stubGetCaseNote(prisonerNumber: String, caseNoteId: String, response: String): StubMapping = stubFor(
    get(urlPathEqualTo("/case-notes/$prisonerNumber/$caseNoteId"))
      .willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withStatus(200)
          .withBody(response),
      ),
  )

  fun stubGetCaseNote(prisonerNumber: String, caseNoteId: String, status: Int = 500): StubMapping = stubFor(
    get(urlPathEqualTo("/case-notes/$prisonerNumber/$caseNoteId"))
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
              "userMessage": "Internal Server Error",
              "developerMessage": "No database connection",
              "moreInfo": null
            }
            """,
          ),
      ),
  )

  fun stubGetCaseNotes(request: String, response: String): StubMapping = stubFor(
    post(urlPathEqualTo("/search/case-notes/by-ids"))
      .withRequestBody(equalToJson(request))
      .willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withStatus(200)
          .withBody(response),
      ),
  )

  fun stubGetCaseNotes(status: Int = 500): StubMapping = stubFor(
    post(urlPathEqualTo("/search/case-notes/by-ids"))
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
              "userMessage": "Internal Server Error",
              "developerMessage": "No database connection",
              "moreInfo": null
            }
            """,
          ),
      ),
  )
}
