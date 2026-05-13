package uk.gov.justice.digital.hmpps.xraybodyscansapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class XRayBodyScansApi

fun main(args: Array<String>) {
  runApplication<XRayBodyScansApi>(*args)
}
