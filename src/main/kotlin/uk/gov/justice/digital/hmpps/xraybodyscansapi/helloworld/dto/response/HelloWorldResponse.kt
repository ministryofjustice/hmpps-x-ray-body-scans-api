package uk.gov.justice.digital.hmpps.xraybodyscansapi.helloworld.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Hello world response")
data class HelloWorldResponse(
  @Schema(description = "The greeting message", example = "Hello, World!")
  val message: String,
)
