package uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.xraybodyscansapi.config.ROLE_X_RAY_BODY_SCANS_API__SCAN_CASE_NOTE__RO
import uk.gov.justice.digital.hmpps.xraybodyscansapi.config.ROLE_X_RAY_BODY_SCANS_API__SCAN_CASE_NOTE__RW
import uk.gov.justice.digital.hmpps.xraybodyscansapi.config.RequireReadScanNoteRole
import uk.gov.justice.digital.hmpps.xraybodyscansapi.config.RequireWriteScanNoteRole
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.request.CreateScanCaseNoteRequest
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.response.ScanCaseNoteResponse
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.service.ScanService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.util.UUID

@RestController
@Tag(
  name = "X-Ray Body Scan Case Notes",
  description = "Endpoints for managing prisoner x-ray body scan case notes.",
)
@RequestMapping(
  value = ["/scan/{scanId}/case-note"],
  produces = [MediaType.APPLICATION_JSON_VALUE],
)
class CaseNoteResource(
  private val scanService: ScanService,
) {
  @GetMapping
  @RequireReadScanNoteRole
  @Operation(
    summary = "Get the case note associated with an x-ray body scan",
    description = "Returns the case note for the given scan. The scan must have an associated case note.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Case note returned successfully.",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized. Missing or invalid token.",
        content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden. Token does not have the role $ROLE_X_RAY_BODY_SCANS_API__SCAN_CASE_NOTE__RO or $ROLE_X_RAY_BODY_SCANS_API__SCAN_CASE_NOTE__RW.",
        content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Scan not found or has no associated case note.",
        content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "500",
        description = "Internal server error.",
        content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getScanCaseNote(
    @PathVariable scanId: UUID,
  ): ScanCaseNoteResponse = scanService.getScanCaseNote(scanId)

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @RequireWriteScanNoteRole
  @Operation(
    summary = "Create a case note for an x-ray body scan",
    description = "Creates a case note associated with the given x-ray body scan.",
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Case note successfully created.",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Invalid request or malformed body.",
        content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized. Missing or invalid token.",
        content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden. Token does not have the role $ROLE_X_RAY_BODY_SCANS_API__SCAN_CASE_NOTE__RW.",
        content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Scan not found.",
        content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "500",
        description = "Internal server error.",
        content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun createCaseNote(
    @PathVariable scanId: UUID,
    @RequestBody @Valid request: CreateScanCaseNoteRequest,
  ) = scanService.createCaseNote(scanId, request)
}
