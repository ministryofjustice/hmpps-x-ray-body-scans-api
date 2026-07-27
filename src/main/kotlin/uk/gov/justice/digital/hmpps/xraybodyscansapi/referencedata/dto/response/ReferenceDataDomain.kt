package uk.gov.justice.digital.hmpps.xraybodyscansapi.referencedata.dto.response

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.xraybodyscansapi.referencedata.repository.ReferenceDataDomainEntity
import java.time.LocalDateTime

@Schema(description = "Domains of data referenced in x-ray body scan records")
data class ReferenceDataDomain(
  @Schema(
    description = "Internal code",
    type = "string",
    requiredMode = Schema.RequiredMode.REQUIRED,
  )
  val code: String,
  @Schema(
    description = "Human-readable description",
    type = "string",
    requiredMode = Schema.RequiredMode.REQUIRED,
  )
  val description: String,
  @Schema(
    description = "When the domain was created",
    type = "string",
    format = "date-time",
    requiredMode = Schema.RequiredMode.REQUIRED,
  )
  val createdAt: LocalDateTime,
  @Schema(
    description = "Who created the domain",
    type = "string",
    requiredMode = Schema.RequiredMode.REQUIRED,
  )
  val createdBy: String,
  @Schema(
    description = "When the domain was updated",
    type = "string",
    format = "date-time",
    requiredMode = Schema.RequiredMode.REQUIRED,
  )
  val lastModifiedAt: LocalDateTime,
  @Schema(
    description = "Who updated the domain",
    type = "string",
    requiredMode = Schema.RequiredMode.REQUIRED,
  )
  val lastModifiedBy: String,
  @Schema(
    description = "When the domain was deactivated/deleted, null if still active",
    type = "string",
    format = "date-time",
    nullable = true,
    requiredMode = Schema.RequiredMode.NOT_REQUIRED,
  )
  val deactivatedAt: LocalDateTime?,
  @Schema(
    description = "Who deactivated/deleted the domain, null if still active",
    type = "string",
    nullable = true,
    requiredMode = Schema.RequiredMode.NOT_REQUIRED,
  )
  val deactivatedBy: String?,
  @Schema(
    description = "Values in the domain",
    requiredMode = Schema.RequiredMode.REQUIRED,
  )
  val codes: List<ReferenceDataCode>,
) {
  constructor(entity: ReferenceDataDomainEntity) : this(
    code = entity.code,
    description = entity.description,
    createdAt = entity.createdAt,
    createdBy = entity.createdBy,
    lastModifiedAt = entity.lastModifiedAt,
    lastModifiedBy = entity.lastModifiedBy,
    deactivatedAt = entity.deactivatedAt,
    deactivatedBy = entity.deactivatedBy,
    codes = entity.codes.map(::ReferenceDataCode),
  )

  @get:Schema(description = "Whether this domain has been deactivated; effectively deleted")
  @get:JsonProperty
  val active: Boolean
    get() = deactivatedAt == null
}
