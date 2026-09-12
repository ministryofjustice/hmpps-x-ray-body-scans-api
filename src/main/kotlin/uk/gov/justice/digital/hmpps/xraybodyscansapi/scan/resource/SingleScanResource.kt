package uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.xraybodyscansapi.config.ADMIN_ROLE
import uk.gov.justice.digital.hmpps.xraybodyscansapi.config.READ_ROLE
import uk.gov.justice.digital.hmpps.xraybodyscansapi.config.RequireAdminRole
import uk.gov.justice.digital.hmpps.xraybodyscansapi.config.RequireReadRole
import uk.gov.justice.digital.hmpps.xraybodyscansapi.config.WRITE_ROLE
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.request.DeleteScanRequest
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
  value = ["/scan/{scanId}"],
  produces = [MediaType.APPLICATION_JSON_VALUE],
)
class SingleScanResource(
  private val scanService: ScanService,
) {
  @GetMapping
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
        description = "Forbidden. Token does not have the role $READ_ROLE or $WRITE_ROLE or $ADMIN_ROLE.",
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

  @DeleteMapping
  @RequireAdminRole
  @Operation(
    summary = "Delete an x-ray body scan recorded in DPS by id",
    description = "Scans should not normally be deleted and this should only be used for administrative purposes. " +
      "The information is soft-deleted; it remains in storage but is no longer accessible.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Scan deleted successfully.",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Invalid request. Reason is blank or missing.",
        content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized. Missing or invalid token.",
        content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden. Token does not have the role $ADMIN_ROLE.",
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
  fun deleteScan(
    @PathVariable
    scanId: UUID,
    @ParameterObject
    @Valid
    request: DeleteScanRequest,
  ): ResponseEntity<ScanResponse> = ResponseEntity.ofNullable(
    scanService.deleteScans(listOf(scanId), request.reason).firstOrNull(),
  )
}
