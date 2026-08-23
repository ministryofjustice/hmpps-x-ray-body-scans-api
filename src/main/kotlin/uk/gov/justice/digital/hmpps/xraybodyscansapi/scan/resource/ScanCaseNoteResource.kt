package uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Pattern
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.xraybodyscansapi.config.ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RW
import uk.gov.justice.digital.hmpps.xraybodyscansapi.config.RequireWriteRole
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.request.CreateScanCaseNoteRequest
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.service.ScanService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.util.UUID

@RestController
@Tag(
  name = "X-Ray Body Scans",
  description = "Endpoints for managing prisoner x-ray body scans.",
)
@RequestMapping(
  value = ["/prisoner/{prisonerNumber}/scan/{scanId}/case-note"],
  produces = [MediaType.APPLICATION_JSON_VALUE],
)
class ScanCaseNoteResource(
  private val scanService: ScanService,
) {
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @RequireWriteRole
  @Operation(
    summary = "Create a case note for an x-ray body scan",
    description = "Creates a case note associated with the given x-ray body scan for the prisoner.",
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Case note successfully created.",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized. Missing or invalid token.",
        content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden. Token does not have the role $ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RW.",
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
    @PathVariable
    @Pattern(
      regexp = "^[A-Z]\\d{4}[A-Z]{2}$",
      message = "prisonerNumber must be in the right form, e.g. A1234BC.",
    )
    prisonerNumber: String,
    @PathVariable
    scanId: UUID,
    @RequestBody
    @Valid
    request: CreateScanCaseNoteRequest,
  ) = scanService.createCaseNote(prisonerNumber, scanId, request)
}
