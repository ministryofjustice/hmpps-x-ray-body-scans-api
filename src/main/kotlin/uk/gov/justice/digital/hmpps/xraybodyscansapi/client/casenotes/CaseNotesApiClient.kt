package uk.gov.justice.digital.hmpps.xraybodyscansapi.client.casenotes

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import uk.gov.justice.digital.hmpps.xraybodyscansapi.client.casenotes.request.CreateCaseNoteRequest
import uk.gov.justice.digital.hmpps.xraybodyscansapi.client.casenotes.response.CaseNoteResponse
import uk.gov.justice.digital.hmpps.xraybodyscansapi.config.DownstreamServiceException

@Component
class CaseNotesApiClient(
  @Qualifier("caseNotesApiWebClient") private val webClient: WebClient,
) {
  fun createCaseNote(prisonerNumber: String, request: CreateCaseNoteRequest): CaseNoteResponse = try {
    webClient
      .post()
      .uri("/case-notes/{prisonerNumber}", prisonerNumber)
      .bodyValue(request)
      .retrieve()
      .bodyToMono<CaseNoteResponse>()
      .block()!!
  } catch (e: Exception) {
    throw DownstreamServiceException("Case Notes API create case note request failed", e)
  }

  fun getCaseNote(prisonerNumber: String, caseNoteId: String): CaseNoteResponse = try {
    webClient
      .get()
      .uri("/case-notes/{prisonerNumber}/{caseNoteId}", prisonerNumber, caseNoteId)
      .retrieve()
      .bodyToMono<CaseNoteResponse>()
      .block()!!
  } catch (e: Exception) {
    throw DownstreamServiceException("Case Notes API get case note request failed", e)
  }
}
