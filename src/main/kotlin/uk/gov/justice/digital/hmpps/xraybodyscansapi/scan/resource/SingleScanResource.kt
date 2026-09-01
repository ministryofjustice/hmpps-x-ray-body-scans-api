package uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.xraybodyscansapi.config.ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RO
import uk.gov.justice.digital.hmpps.xraybodyscansapi.config.ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RW
import uk.gov.justice.digital.hmpps.xraybodyscansapi.config.RequireReadRole
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.response.ScanResponse
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.service.ScanService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.util.UUID

@RestController
@Tag(
  name = "X-Ray Body Scans",
  description = "Endpoints for managing prisoner x-ray body scans.",
)
@RequestMapping(
  produces = [MediaType.APPLICATION_JSON_VALUE],
)
class SingleScanResource(
  private val scanService: ScanService,
) {
  @GetMapping("/scans/{scanId}")
  @RequireReadRole
  @Operation(
    summary = "Retrieve an x-ray body scan recorded in DPS by id",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Scan returned successfully.",
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
        responseCode = "404",
        description = "Not found.",
        content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "500",
        description = "Internal server error.",
        content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getScan(
    @PathVariable
    scanId: UUID,
  ): ResponseEntity<ScanResponse> = ResponseEntity.ofNullable(scanService.getScans(listOf(scanId)).firstOrNull())
}
