package uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Pattern
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.xraybodyscansapi.config.ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RO
import uk.gov.justice.digital.hmpps.xraybodyscansapi.config.ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RW
import uk.gov.justice.digital.hmpps.xraybodyscansapi.config.RequireReadRole
import uk.gov.justice.digital.hmpps.xraybodyscansapi.config.RequireWriteRole
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.request.CreateScanRequest
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.request.ListScansRequest
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.response.ScanResponse
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.response.ScanSummaryResponse
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.service.ScanService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestController
@Tag(
  name = "X-Ray Body Scans",
  description = "Endpoints for managing prisoner x-ray body scans.",
)
@RequestMapping(
  value = ["/prisoner/{prisonerNumber}/scan"],
  produces = [MediaType.APPLICATION_JSON_VALUE],
)
class ScanResource(
  private val scanService: ScanService,
) {
  @GetMapping
  @RequireReadRole
  @Operation(
    summary = "Retrieve x-ray body scans for a prisoner",
    description = "Returns recorded x-ray body scans for the given prisoner. " +
      "If the prisoner is not found, the list is empty. " +
      "Ensure the prisoner exists prior to use.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Scans returned successfully.",
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
  fun listScans(
    @PathVariable
    @Pattern(
      regexp = "^[A-Z]\\d{4}[A-Z]{2}$",
      message = "prisonerNumber must be in the right form, e.g. A1234BC.",
    )
    prisonerNumber: String,
    @ParameterObject
    @Valid
    query: ListScansRequest? = null,
    @ParameterObject
    @PageableDefault(page = 0, size = 20, sort = ["scanDate"], direction = Sort.Direction.DESC)
    pageable: Pageable,
  ): Page<ScanResponse> = scanService.listScans(prisonerNumber, query, pageable)

  @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
  @ResponseStatus(HttpStatus.CREATED)
  @RequireWriteRole
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
        description = "Forbidden. Token does not have the role $ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RW.",
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
    @RequestBody
    @Valid
    request: CreateScanRequest,
  ): ResponseEntity<ScanResponse> = ResponseEntity
    .status(HttpStatus.CREATED)
    .body(scanService.createScan(prisonerNumber, request))

  @GetMapping("/summary")
  @RequireReadRole
  @Operation(
    summary = "Count x-ray body scans for a prisoner",
    description = "Returns the total number of x-ray body scans for the given prisoner this calendar year and how many remain. " +
      "If the prisoner is not found, the count will default to zero. " +
      "Ensure the prisoner exists prior to use.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Count returned successfully.",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Invalid request. Check the prisoner number and dates.",
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
  fun summariseScans(
    @PathVariable
    @Pattern(
      regexp = "^[A-Z]\\d{4}[A-Z]{2}$",
      message = "prisonerNumber must be in the right form, e.g. A1234BC.",
    )
    prisonerNumber: String,
  ): ScanSummaryResponse = scanService.summariseScans(prisonerNumber)
}
