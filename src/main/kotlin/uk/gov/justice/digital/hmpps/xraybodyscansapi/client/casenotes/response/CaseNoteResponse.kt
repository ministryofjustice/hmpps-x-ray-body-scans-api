package uk.gov.justice.digital.hmpps.xraybodyscansapi.client.casenotes.response

import java.time.LocalDateTime

data class CaseNoteResponse(
  val caseNoteId: String,
  val offenderIdentifier: String,
  val type: String,
  val typeDescription: String,
  val subType: String,
  val subTypeDescription: String,
  val text: String,
  val creationDateTime: LocalDateTime,
  val occurrenceDateTime: LocalDateTime,
  val authorName: String,
)
