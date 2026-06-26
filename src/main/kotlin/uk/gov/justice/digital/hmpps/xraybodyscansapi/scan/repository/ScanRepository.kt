package uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface ScanRepository : JpaRepository<ScanEntity, Long> {
  // TODO: add specification filters and make paged response
  fun findByPrisonerNumberIn(prisonerNumbers: List<String>): List<ScanEntity>

  fun findByPrisonerNumberInAndScanDateBetween(
    prisonerNumbers: List<String>,
    fromScanDate: LocalDate,
    toScanDate: LocalDate,
  ): List<ScanEntity>
}
