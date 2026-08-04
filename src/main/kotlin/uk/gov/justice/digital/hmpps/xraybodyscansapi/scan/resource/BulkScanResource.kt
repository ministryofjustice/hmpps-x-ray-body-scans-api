package uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.xraybodyscansapi.config.ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RO
import uk.gov.justice.digital.hmpps.xraybodyscansapi.config.ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RW
import uk.gov.justice.digital.hmpps.xraybodyscansapi.config.RequireReadRole
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.request.BulkScanSummaryRequest
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.response.ScanSummaryResponse
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.service.ScanService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestController
@Tag(
  name = "Bulk X-Ray Body Scans",
  description = "Endpoints for managing x-ray body scans across multiple prisoners.",
)
@RequestMapping(
  value = ["/bulk"],
  produces = [MediaType.APPLICATION_JSON_VALUE],
)
class BulkScanResource(
  private val scanService: ScanService,
) {
  @PostMapping("/summary", consumes = [MediaType.APPLICATION_JSON_VALUE])
  @RequireReadRole
  @Operation(
    summary = "Summarise x-ray body scans for multiple prisoners",
    description = "Returns scan summaries for each of the given prisoners for this calendar year. " +
      "If a prisoner is not found, their counts will default to zero. " +
      "Ensure prisoners exist prior to use.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Summaries returned successfully.",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Invalid request or malformed body. Check request schema.",
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
  fun bulkSummariseScans(
    @RequestBody
    @Valid
    request: BulkScanSummaryRequest,
  ): List<ScanSummaryResponse> = scanService.summariseScans(request.prisonerNumbers, request.includeAlerts)
}
