package uk.gov.justice.digital.hmpps.xraybodyscansapi.config

import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.validation.FieldError
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.resource.NoResourceFoundException
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestControllerAdvice
class XRayBodyScansApiExceptionHandler {
  @ExceptionHandler(ValidationException::class)
  fun handleValidationException(e: ValidationException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(BAD_REQUEST)
    .body(
      ErrorResponse(
        status = BAD_REQUEST,
        userMessage = "Validation failure: ${e.message}",
        developerMessage = e.message,
      ),
    ).also { log.info("Validation exception: {}", e.message) }

  @ExceptionHandler(NotFoundException::class)
  fun handleNotFoundException(e: NotFoundException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(NOT_FOUND)
    .body(
      ErrorResponse(
        status = NOT_FOUND,
        userMessage = "Not found: ${e.message}",
        developerMessage = e.message,
      ),
    ).also { log.info("Entity not found exception: {}", e.message) }

  @ExceptionHandler(NoResourceFoundException::class)
  fun handleNoResourceFoundException(e: NoResourceFoundException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(NOT_FOUND)
    .body(
      ErrorResponse(
        status = NOT_FOUND,
        userMessage = "No resource found failure: ${e.message}",
        developerMessage = e.message,
      ),
    ).also { log.info("No resource found exception: {}", e.message) }

  @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
  fun handleMethodNotAllowedException(e: HttpRequestMethodNotSupportedException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(METHOD_NOT_ALLOWED)
    .body(
      ErrorResponse(
        status = METHOD_NOT_ALLOWED,
        userMessage = "Method not allowed: ${e.message}",
        developerMessage = e.message,
      ),
    ).also { log.info("Method not allowed: {}", e.message) }

  @ExceptionHandler(AccessDeniedException::class)
  fun handleAccessDeniedException(e: AccessDeniedException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(FORBIDDEN)
    .body(
      ErrorResponse(
        status = FORBIDDEN,
        userMessage = "Forbidden: ${e.message}",
        developerMessage = e.message,
      ),
    ).also { log.debug("Forbidden (403) returned: {}", e.message) }

  @ExceptionHandler(Exception::class)
  fun handleException(e: Exception): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(INTERNAL_SERVER_ERROR)
    .body(
      ErrorResponse(
        status = INTERNAL_SERVER_ERROR,
        userMessage = "Unexpected error: ${e.message}",
        developerMessage = e.message,
      ),
    ).also { log.error("Unexpected exception", e) }

  @ExceptionHandler(MethodArgumentTypeMismatchException::class)
  fun handleMethodArgumentTypeMismatchException(e: MethodArgumentTypeMismatchException): ResponseEntity<ErrorResponse> {
    val type = e.requiredType
    val message = if (type!!.isEnum) {
      "Parameter ${e.name} must be one of the following ${type.enumConstants.joinToString(", ")}"
    } else {
      "Parameter ${e.name} must be of type ${type.typeName}"
    }
    return ResponseEntity
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST,
          userMessage = "Validation failure: $message",
          developerMessage = e.message,
        ),
      ).also { log.info("Validation exception: {}", message) }
  }

  @ExceptionHandler(MethodArgumentNotValidException::class)
  fun handleInvalidMethodArgumentException(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
    val message = e.allErrors.joinToString(", ") {
      val field = if (it is FieldError) {
        "${it.objectName}.${it.field}"
      } else {
        it.objectName
      }
      "$field: ${it.defaultMessage}"
    }
    return ResponseEntity
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST,
          userMessage = "Validation failure: $message",
          developerMessage = message,
        ),
      ).also { log.info("MethodArgumentNotValidException exception: {}", message) }
  }

  @ExceptionHandler(HandlerMethodValidationException::class)
  fun handleHandlerMethodValidationException(
    e: HandlerMethodValidationException,
  ): ResponseEntity<ErrorResponse> {
    val message = e.parameterValidationResults
      .flatMap { it.resolvableErrors }
      .mapNotNull { it.defaultMessage }
      .joinToString("; ")
      .ifBlank { "Validation failure" }

    return ResponseEntity
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST,
          userMessage = "Validation failure",
          developerMessage = message,
        ),
      ).also { log.info("Validation exception: {}", message) }
  }

  @ExceptionHandler(HttpMessageNotReadableException::class)
  fun handleHttpMessageNotReadableException(
    e: HttpMessageNotReadableException,
  ): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(BAD_REQUEST)
    .body(
      ErrorResponse(
        status = BAD_REQUEST,
        userMessage = "Malformed request body",
        developerMessage = e.message,
      ),
    ).also { log.info("Could not read HTTP message: {}", e.message) }

  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

// In case we want to handle them differently later down the line:
class DownstreamServiceException(message: String, cause: Throwable) : Exception(message, cause)

class NotFoundException(message: String) : Exception(message)
