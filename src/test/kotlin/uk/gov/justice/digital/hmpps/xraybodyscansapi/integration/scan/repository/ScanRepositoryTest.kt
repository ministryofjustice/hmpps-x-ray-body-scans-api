package uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.scan.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.xraybodyscansapi.referencedata.dto.response.ReferenceDataDomains
import uk.gov.justice.digital.hmpps.xraybodyscansapi.referencedata.repository.ReferenceDataCodeRepository
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.repository.ScanEntity
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.repository.ScanRepository
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.repository.groupOutcomes
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters.firstDayOfYear

@DataJpaTest
@ActiveProfiles("test")
class ScanRepositoryTest {
  @Autowired
  private lateinit var codeRepository: ReferenceDataCodeRepository

  @Autowired
  private lateinit var scanRepository: ScanRepository

  private val prisonerNumber = "A1111AA"

  private val today = LocalDate.now()
  private val startOfYear = today.with(firstDayOfYear())
  private val scanDate: LocalDate = today.minusDays(1)

  @Test
  fun `save persists a scan correctly`() {
    val before = LocalDateTime.now().minusSeconds(10)
    val after = LocalDateTime.now().plusSeconds(10)

    val saved = scanRepository.save(scanEntity())

    assertThat(saved.id).isNotNull()
    assertThat(saved.createdAt).isNotNull()
    assertThat(saved.createdAt).isBetween(before, after)
    assertThat(saved.lastModifiedAt).isBetween(before, after)

    val found = scanRepository.findById(saved.id).orElseThrow()
    assertThat(found.prisonerNumber).isEqualTo(prisonerNumber)
    assertThat(found.prisonId).isEqualTo("MDI")
    assertThat(found.scanDate).isEqualTo(scanDate)
    assertThat(found.justification.code).isEqualTo("REASONABLE_SUSPICION")
    assertThat(found.outcome.code).isEqualTo("NEGATIVE")
    assertThat(found.typeOfFind).isNull()
    assertThat(found.createdBy).isEqualTo("abc12a")
  }

  @Nested
  inner class ScanSummaries {
    @Test
    fun `empty summary`() {
      val summary = scanRepository.scanSummaryRowsForPrisoners(listOf(prisonerNumber), startOfYear, today).groupOutcomes()
      assertThat(summary).isEmpty()
    }

    @Test
    fun `summarise scans`() {
      scanRepository.saveAll(
        listOf(
          scanEntity(prisonerNumber, outcome = "POSITIVE", typeOfFind = "NOT_KNOWN"),
          scanEntity("B2222BB", outcome = "INCONCLUSIVE"),
          scanEntity("C3333CC", outcome = "INCONCLUSIVE"),
          scanEntity(prisonerNumber, outcome = "NEGATIVE"),
          scanEntity(prisonerNumber, outcome = "POSITIVE", typeOfFind = "INORGANIC"),
          scanEntity(prisonerNumber, scanDate = startOfYear.minusDays(1), outcome = "POSITIVE", typeOfFind = "INORGANIC"),
        ),
      )

      val summary = scanRepository.scanSummaryRowsForPrisoners(listOf(prisonerNumber, "B2222BB"), startOfYear, today).groupOutcomes()
      assertThat(summary).isEqualTo(
        mapOf(
          prisonerNumber to mapOf(
            "POSITIVE" to 2,
            "NEGATIVE" to 1,
          ),
          "B2222BB" to mapOf("INCONCLUSIVE" to 1),
        ),
      )
    }
  }

  private fun scanEntity(
    prisonerNumber: String = this.prisonerNumber,
    scanDate: LocalDate = this.scanDate,
    justification: String = "REASONABLE_SUSPICION",
    outcome: String = "NEGATIVE",
    typeOfFind: String? = null,
  ) = ScanEntity(
    prisonerNumber = prisonerNumber,
    prisonId = "MDI",
    scanDate = scanDate,
    justification = codeRepository.findByDomainAndCode(ReferenceDataDomains.JUSTIFICATION, justification)!!,
    outcome = codeRepository.findByDomainAndCode(ReferenceDataDomains.OUTCOME, outcome)!!,
    typeOfFind = typeOfFind?.let {
      codeRepository.findByDomainAndCode(ReferenceDataDomains.TYPE_OF_FIND, typeOfFind)!!
    },
    createdBy = "abc12a",
  )
}
