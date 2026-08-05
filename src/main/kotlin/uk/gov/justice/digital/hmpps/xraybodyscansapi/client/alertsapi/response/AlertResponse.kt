package uk.gov.justice.digital.hmpps.xraybodyscansapi.client.alertsapi.response

data class AlertResponse(
  val content: List<Alert>,
) {
  fun toList(): List<Alert> = content
}

data class Alert(
  val alertUuid: String, // technically a uuid, but can be passed transparently to clients
  val prisonNumber: String,
  val alertCode: AlertCode,
  val description: String? = null,
  // NB: more fields exist
)

data class AlertCode(
  val alertTypeCode: String,
  val alertTypeDescription: String,
  val code: String,
  val description: String,
  // NB: more fields exist
)
