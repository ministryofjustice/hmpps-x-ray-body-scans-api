package uk.gov.justice.digital.hmpps.xraybodyscansapi.client.casenotes.request

import java.time.LocalDateTime

data class CreateCaseNoteRequest(
  val type: String,
  val subType: String,
  val text: String,
  val occurrenceDateTime: LocalDateTime? = null,
)
