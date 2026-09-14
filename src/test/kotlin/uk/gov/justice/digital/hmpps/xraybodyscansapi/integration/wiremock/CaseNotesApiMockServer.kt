package uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
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

  fun stubGetCaseNote(
    body: String,
    prisonerNumber: String,
    caseNoteId: String,
  ): StubMapping = stubFor(
    get(urlPathEqualTo("/case-notes/$prisonerNumber/$caseNoteId"))
      .willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withStatus(200)
          .withBody(body),
      ),
  )
}
