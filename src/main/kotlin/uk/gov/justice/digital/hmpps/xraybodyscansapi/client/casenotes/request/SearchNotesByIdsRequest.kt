package uk.gov.justice.digital.hmpps.xraybodyscansapi.client.casenotes.request

data class SearchNotesByIdsRequest(
  val ids: Collection<String>,
)
