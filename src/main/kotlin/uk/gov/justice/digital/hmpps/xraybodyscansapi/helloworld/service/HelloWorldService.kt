package uk.gov.justice.digital.hmpps.xraybodyscansapi.helloworld.service

import org.springframework.stereotype.Service

@Service
class HelloWorldService {
  fun getHelloMessage(name: String): String = "Hello, world! Specifically you, $name!"
}
