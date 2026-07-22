package uk.gov.justice.digital.hmpps.xraybodyscansapi.referencedata.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.xraybodyscansapi.config.ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RO
import uk.gov.justice.digital.hmpps.xraybodyscansapi.config.ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RW
import uk.gov.justice.digital.hmpps.xraybodyscansapi.config.RequireReadRole
import uk.gov.justice.digital.hmpps.xraybodyscansapi.referencedata.dto.response.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.xraybodyscansapi.referencedata.service.ReferenceDataService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestController
@Tag(
  name = "Reference data",
  description = "Values referenced in x-ray body scan records.",
)
@RequestMapping(
  value = ["/reference-data"],
  produces = [MediaType.APPLICATION_JSON_VALUE],
)
class ReferenceDataResource(
  private val referenceDataService: ReferenceDataService,
) {
  @GetMapping
  @RequireReadRole
  @Operation(
    summary = "Retrieve all data referenced in x-ray body scans",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Data returned successfully.",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Invalid request. Check the prisoner number and filters.",
        content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized. Missing or invalid token.",
        content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden. Token does not have the role $ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RO or $ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RW.",
        content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "500",
        description = "Internal server error.",
        content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getAllReferenceData(): Map<String, ReferenceDataDomain> = referenceDataService.getAllReferenceData()
}
