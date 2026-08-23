package uk.gov.justice.digital.hmpps.xraybodyscansapi.client.casenotes.response

import java.time.LocalDateTime

data class CaseNoteResponse(
  val caseNoteId: String,
  val offenderIdentifier: String,
  val type: String,
  val subType: String,
  val text: String,
  val occurrenceDateTime: LocalDateTime,
)
