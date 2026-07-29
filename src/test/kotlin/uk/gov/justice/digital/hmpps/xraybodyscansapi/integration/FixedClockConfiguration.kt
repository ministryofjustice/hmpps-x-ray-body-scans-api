package uk.gov.justice.digital.hmpps.xraybodyscansapi.integration

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import java.time.Clock

/**
 * Sets the clock to 27 July 2026 9:10:11 am BST in tests
 */
@TestConfiguration
class FixedClockConfiguration {
  companion object : FixedClock()

  @Primary
  @Bean
  fun fixedClock(): Clock = clock
}
