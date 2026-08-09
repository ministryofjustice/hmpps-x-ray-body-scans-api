package uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.repository

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.UUID

@Repository
interface ScanRepository :
  JpaRepository<ScanEntity, UUID>,
  JpaSpecificationExecutor<ScanEntity> {
  /** Find scans by id and eagerly load reference data */
  @EntityGraph(attributePaths = ["justification", "outcome", "typeOfFind"])
  fun findByIdIn(scanIds: List<UUID>): List<ScanEntity>

  /** Summarise scan outcomes for given period and prisoners */
  @Query(
    """
    select prisonerNumber as prisonerNumber, outcome.code as outcome, count(*) as count
    from ScanEntity
    where prisonerNumber in :prisonerNumbers
    and scanDate between :fromScanDate and :toScanDate
    group by prisonerNumber, outcome
    """,
  )
  fun scanSummaryRowsForPrisoners(
    prisonerNumbers: List<String>,
    fromScanDate: LocalDate,
    toScanDate: LocalDate,
  ): List<ScanSummaryRow>
}
