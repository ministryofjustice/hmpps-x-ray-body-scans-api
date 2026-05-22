package uk.gov.justice.digital.hmpps.xraybodyscansapi.helloworld.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.xraybodyscansapi.helloworld.dto.response.HelloWorldResponse
import uk.gov.justice.digital.hmpps.xraybodyscansapi.helloworld.service.HelloWorldService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestController
@Tag(
  name = "Hello World",
  description = "A simple hello world endpoint.",
)
@RequestMapping(
  value = ["v1/hello"],
  produces = [MediaType.APPLICATION_JSON_VALUE],
)
class HelloWorldResource(
  private val helloWorldService: HelloWorldService,
) {

  @GetMapping("/{name}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    description = "Returns a hello world message for the given name.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Hello world message successfully returned.",
      ),
      ApiResponse(
        responseCode = "500",
        description = "Internal server error",
        content = [
          Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  @PreAuthorize("hasRole('ROLE_HELLO_WORLD'")
  fun getHelloWorld(
    @PathVariable name: String,
  ): ResponseEntity<HelloWorldResponse> = ResponseEntity.ok(
    HelloWorldResponse(message = helloWorldService.getHelloMessage(name)),
  )
}
