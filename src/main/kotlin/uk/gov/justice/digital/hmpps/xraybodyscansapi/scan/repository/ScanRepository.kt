package uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.UUID

@Repository
interface ScanRepository :
  JpaRepository<ScanEntity, UUID>,
  JpaSpecificationExecutor<ScanEntity> {
  fun findByPrisonerNumberInAndScanDateBetween(
    prisonerNumbers: List<String>,
    fromScanDate: LocalDate,
    toScanDate: LocalDate,
  ): List<ScanEntity>
}
