package uk.gov.justice.digital.hmpps.xraybodyscansapi.client.casenotes

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.xraybodyscansapi.client.casenotes.request.CreateCaseNoteRequest
import uk.gov.justice.digital.hmpps.xraybodyscansapi.client.casenotes.response.CaseNoteResponse
import uk.gov.justice.digital.hmpps.xraybodyscansapi.config.DownstreamServiceException
import uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.wiremock.CaseNotesApiExtension
import uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.wiremock.CaseNotesApiExtension.Companion.caseNotesApi
import java.time.LocalDateTime

@ExtendWith(CaseNotesApiExtension::class)
class CaseNotesApiClientTest {
  private lateinit var client: CaseNotesApiClient

  @BeforeEach
  fun resetMocks() {
    val webClient = WebClient.create("http://localhost:${caseNotesApi.port()}")
    client = CaseNotesApiClient(webClient)
  }

  @Test
  fun `createCaseNote sends the request and returns the case note`() {
    caseNotesApi.stubCreateCaseNote("A1234BC", caseNoteResponse)
    val result = client.createCaseNote(
      "A1234BC",
      CreateCaseNoteRequest(
        type = "GEN",
        subType = "XRBS",
        text = "notes",
        locationId = "MDI",
        occurrenceDateTime = LocalDateTime.of(2026, 7, 26, 0, 0),
      ),
    )
    verifyCaseNote(result)
  }

  @Test
  fun `getCaseNote returns a case note`() {
    caseNotesApi.stubGetCaseNote("A1234BC", "341c845e-fadc-4ec8-9330-81c83968c1a8", caseNoteResponse)
    val result = client.getCaseNote("A1234BC", "341c845e-fadc-4ec8-9330-81c83968c1a8")
    verifyCaseNote(result)
  }

  @Test
  fun `getCaseNotes returns case notes`() {
    caseNotesApi.stubGetCaseNotes(
      // language=json
      """
      [
        $caseNoteResponse,
        {
          "caseNoteId": "eb1393f6-3db2-400f-bd37-2635111ddb69",
          "offenderIdentifier": "A1234BC",
          "type": "GEN",
          "typeDescription": "General",
          "subType": "XRBS",
          "subTypeDescription": "X-ray body scan",
          "text": "notes",
          "creationDateTime": "2026-07-21T09:10:11",
          "occurrenceDateTime": "2026-07-20T00:00:00",
          "authorName": "A User",
          "amendments": []
        }
      ]
      """,
    )
    val results = client.getCaseNotes(listOf("341c845e-fadc-4ec8-9330-81c83968c1a8", "eb1393f6-3db2-400f-bd37-2635111ddb69", "2ec4f3da-c8a4-43cc-bb06-fd9f67e92c37"))
    assertThat(results).hasSize(2)
    verifyCaseNote(results[0])
    assertThat(results[1].amendments).isEmpty()
  }

  private val caseNoteResponse = // language=json
    """
    {
      "caseNoteId": "341c845e-fadc-4ec8-9330-81c83968c1a8",
      "offenderIdentifier": "A1234BC",
      "type": "GEN",
      "typeDescription": "General",
      "subType": "XRBS",
      "subTypeDescription": "X-ray body scan",
      "text": "notes",
      "creationDateTime": "2026-07-27T09:10:11",
      "occurrenceDateTime": "2026-07-26T00:00:00",
      "authorName": "A User",
      "amendments": [{
        "additionalNoteText": "more notes",
        "creationDateTime": "2026-07-28T09:10:11",
        "authorName": "Another User"
      }]
    }
    """

  private fun verifyCaseNote(caseNoteResponse: CaseNoteResponse) {
    assertThat(caseNoteResponse.caseNoteId).isEqualTo("341c845e-fadc-4ec8-9330-81c83968c1a8")
    assertThat(caseNoteResponse.creationDateTime).isEqualTo(LocalDateTime.of(2026, 7, 27, 9, 10, 11))
    assertThat(caseNoteResponse.occurrenceDateTime).isEqualTo(LocalDateTime.of(2026, 7, 26, 0, 0))
    assertThat(caseNoteResponse.amendments).hasSize(1)
    assertThat(caseNoteResponse.amendments[0].creationDateTime).isEqualTo(LocalDateTime.of(2026, 7, 28, 9, 10, 11))
  }

  @Test
  fun `createCaseNote returns an error`() {
    caseNotesApi.stubCreateCaseNote("A1234BC", 500)
    assertThatThrownBy {
      client.createCaseNote(
        "A1234BC",
        CreateCaseNoteRequest(
          type = "GEN",
          subType = "XRBS",
          text = "notes",
          locationId = "MDI",
          occurrenceDateTime = LocalDateTime.of(2026, 7, 26, 0, 0),
        ),
      )
    }
      .isInstanceOf(DownstreamServiceException::class.java)
      .hasMessage("Case Notes API create case note request failed")
      .cause()
      .extracting { (it as WebClientResponseException).statusCode.value() }
      .isEqualTo(500)
  }

  @Test
  fun `getCaseNote returns an error`() {
    caseNotesApi.stubGetCaseNote("A1234BC", "341c845e-fadc-4ec8-9330-81c83968c1a8", 500)
    assertThatThrownBy {
      client.getCaseNote("A1234BC", "341c845e-fadc-4ec8-9330-81c83968c1a8")
    }
      .isInstanceOf(DownstreamServiceException::class.java)
      .hasMessage("Case Notes API get case note request failed")
      .cause()
      .extracting { (it as WebClientResponseException).statusCode.value() }
      .isEqualTo(500)
  }

  @Test
  fun `getCaseNotes returns an error`() {
    caseNotesApi.stubGetCaseNotes(500)
    assertThatThrownBy {
      client.getCaseNotes(listOf("341c845e-fadc-4ec8-9330-81c83968c1a8"))
    }
      .isInstanceOf(DownstreamServiceException::class.java)
      .hasMessage("Case Notes API get case notes request failed")
      .cause()
      .extracting { (it as WebClientResponseException).statusCode.value() }
      .isEqualTo(500)
  }
}
