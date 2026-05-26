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
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.request.CreateScanRequest
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.response.CreateScanResponse
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.service.ScanService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestController
@Tag(
  name = "X-Ray Body Scans",
  description = "Endpoints for managing prisoner x-ray body scans.",
)
@RequestMapping(
  value = ["/v1/prisoner/{prisonerNumber}/scan"],
  produces = [MediaType.APPLICATION_JSON_VALUE],
)
class ScanResource(
  private val scanService: ScanService,
) {

  @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('ROLE_SOMETHING')")
  @Operation(
    summary = "Create an x-ray body scan for a prisoner",
    description = "Creates a new x-ray body scan record for the given prisoner.",
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Scan successfully created.",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Invalid request or malformed body. Check request schema.",
        content = [
          Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized. Missing or invalid token.",
        content = [
          Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden. Token does not have the ROLE_SOMETHING role.",
        content = [
          Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "500",
        description = "Internal server error.",
        content = [
          Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun createScan(
    @PathVariable
    @Pattern(
      regexp = "^[A-Z]\\d{4}[A-Z]{2}$",
      message = "prisonerNumber must be in the right form, e.g. A1234BC.",
    )
    prisonerNumber: String,
    @Valid @RequestBody request: CreateScanRequest,
  ): ResponseEntity<CreateScanResponse> = ResponseEntity
    .status(HttpStatus.CREATED)
    .body(scanService.createScan(prisonerNumber, request))
}
