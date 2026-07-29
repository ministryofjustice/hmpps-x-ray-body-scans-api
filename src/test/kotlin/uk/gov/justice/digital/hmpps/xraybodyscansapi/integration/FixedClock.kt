package uk.gov.justice.digital.hmpps.xraybodyscansapi.integration

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters.firstDayOfYear

/**
 * A clock fixed to 27 July 2026 9:10:11 am BST
 */
open class FixedClock {
  val clock: Clock = Clock.fixed(
    Instant.parse("2026-07-27T09:10:11.123+01:00"),
    ZoneId.of("Europe/London"),
  )
  val now: LocalDateTime = LocalDateTime.now(clock)
  val today: LocalDate = LocalDate.now(clock)
  val yearStart: LocalDate = today.with(firstDayOfYear())
}
