package uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Pattern
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.request.CreateScanRequest
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.response.ScanCountResponse
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.response.ScanResponse
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.service.ScanService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters.firstDayOfYear

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

  @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RW')")
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
        description = "Forbidden. Token does not have the role ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RW.",
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
  ): ResponseEntity<ScanResponse> = ResponseEntity
    .status(HttpStatus.CREATED)
    .body(scanService.createScan(prisonerNumber, request))

  @GetMapping("/count")
  @PreAuthorize("hasRole('ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RW')")
  @Operation(
    summary = "Count x-ray body scans for a prisoner",
    description = "Returns the total number of x-ray body scans for the given prisoner.",
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
        description = "Forbidden. Token does not have the role ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RW.",
        content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "500",
        description = "Internal server error.",
        content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun countScans(
    @PathVariable
    @Pattern(
      regexp = "^[A-Z]\\d{4}[A-Z]{2}$",
      message = "prisonerNumber must be in the right form, e.g. A1234BC.",
    )
    prisonerNumber: String,
    @RequestParam(required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Parameter(description = "Count scans on or after this date (YYYY-MM-DD). Defaults to the start of this calendar year.")
    fromStartDate: LocalDate?,
    @RequestParam(required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Parameter(description = "Count scans on or before this date (YYYY-MM-DD). Defaults to today.")
    toStartDate: LocalDate?,
  ): ScanCountResponse {
    val to = toStartDate ?: LocalDate.now()
    val from = fromStartDate ?: to.with(firstDayOfYear())
    return scanService.countScans(prisonerNumber, from, to)
  }
}
