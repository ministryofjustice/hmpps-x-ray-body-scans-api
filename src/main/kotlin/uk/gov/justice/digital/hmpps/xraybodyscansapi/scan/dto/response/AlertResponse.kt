package uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.xraybodyscansapi.client.alertsapi.response.Alert

@Schema(description = "An active alert relevant to x-ray body scanning")
data class AlertResponse(
  @Schema(
    description = "Unique identifier for this alert assignment",
    requiredMode = Schema.RequiredMode.REQUIRED,
  )
  val id: String,
  @Schema(
    description = "Internal type",
    requiredMode = Schema.RequiredMode.REQUIRED,
  )
  val alertType: String,
  @Schema(
    description = "Alert type description",
    requiredMode = Schema.RequiredMode.REQUIRED,
  )
  val typeDescription: String,
  @Schema(
    description = "Internal code",
    requiredMode = Schema.RequiredMode.REQUIRED,
  )
  val alertCode: String,
  @Schema(
    description = "Alert code description",
    requiredMode = Schema.RequiredMode.REQUIRED,
  )
  val codeDescription: String,
) {
  constructor(alert: Alert) : this(
    id = alert.alertUuid,
    alertType = alert.alertCode.alertTypeCode,
    typeDescription = alert.alertCode.alertTypeDescription,
    alertCode = alert.alertCode.code,
    codeDescription = alert.alertCode.description,
  )
}
