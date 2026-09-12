package uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.scan.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
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

  @Test
  fun `get scans by id`() {
    val scanIds = scanRepository.saveAll(
      listOf(
        scanEntity(prisonerNumber, outcome = "POSITIVE", typeOfFind = "NOT_KNOWN"),
        scanEntity(prisonerNumber, outcome = "NEGATIVE"),
        scanEntity(prisonerNumber, outcome = "POSITIVE", typeOfFind = "INORGANIC"),
        scanEntity(prisonerNumber, deleted = LocalDateTime.now() to "Recorded in error"),
      ),
    ).map { it.id }

    val scans = scanRepository.findByDeletedAtIsNullAndIdIn(scanIds)
    assertThat(scans).hasSize(3)
    assertThat(scans).allMatch {
      it.prisonerNumber == prisonerNumber && it.justification.description == "Reasonable suspicion"
    }
  }

  @DisplayName("Latest scans")
  @Nested
  inner class LatestScans {
    @Test
    fun `empty scans list`() {
      val latestScans = scanRepository.latestScansForPrisoners(listOf(prisonerNumber), startOfYear, today)
        .associateBy { it.prisonerNumber }
      assertThat(latestScans).isEmpty()
    }

    @ParameterizedTest(name = "get latest scans {0}")
    @CsvSource(
      value = [
        "excluding deleted ones | false",
        "including deleted ones | true",
      ],
      delimiter = '|',
    )
    fun `get latest scans`(scenario: String, includeDeleted: Boolean) {
      scanRepository.saveAll(
        listOf(
          // person with 1 scan in date range
          scanEntity("A1111AA", justification = "INTELLIGENCE"),

          // person with 3 scans in date range
          scanEntity(
            "B2222BB",
            // least recently created but older scan date
            scanDate = scanDate.minusDays(1),
            outcome = "NEGATIVE",
          ),
          scanEntity(
            "B2222BB",
            // not most recently created but newest scan date
            scanDate = scanDate,
            outcome = "POSITIVE",
          ),
          scanEntity(
            "B2222BB",
            // most recently created but older scan date
            scanDate = scanDate.minusDays(3),
            outcome = "NEGATIVE",
          ),

          // prisoner number not requested
          scanEntity("C3333CC"),

          // date not in range
          scanEntity("D4444DD", scanDate = startOfYear.minusDays(1)),

          // requested, but deleted
          scanEntity("E5555EE", deleted = LocalDateTime.now() to "Recorded in error"),
        ),
      )

      val latestScans = scanRepository.latestScansForPrisoners(
        listOf("A1111AA", "B2222BB", "D4444DD", "E5555EE"),
        startOfYear,
        today,
        includeDeleted,
      ).associateBy { it.prisonerNumber }

      if (includeDeleted) {
        assertThat(latestScans).hasSize(3)
        assertThat(latestScans["E5555EE"]?.deletedReason).isEqualTo("Recorded in error")
      } else {
        assertThat(latestScans).hasSize(2)
      }
      assertThat(latestScans["A1111AA"]?.justification?.description).isEqualTo("Intelligence-led")
      assertThat(latestScans["B2222BB"]?.outcome?.description).isEqualTo("Item detected")
    }
  }

  @DisplayName("Scan summaries")
  @Nested
  inner class ScanSummaries {
    @Test
    fun `empty summary`() {
      val summary = scanRepository.scanSummaryRowsForPrisoners(listOf(prisonerNumber), startOfYear, today)
        .groupOutcomes()
      assertThat(summary).isEmpty()
    }

    @ParameterizedTest(name = "summarise scans {0}")
    @CsvSource(
      value = [
        "excluding deleted ones | false",
        "including deleted ones | true",
      ],
      delimiter = '|',
    )
    fun `summarise scans`(scenario: String, includeDeleted: Boolean) {
      scanRepository.saveAll(
        listOf(
          scanEntity(prisonerNumber, outcome = "POSITIVE", typeOfFind = "NOT_KNOWN"),
          scanEntity("B2222BB", outcome = "INCONCLUSIVE"),
          scanEntity("C3333CC", outcome = "INCONCLUSIVE"),
          scanEntity(prisonerNumber, outcome = "NEGATIVE"),
          scanEntity("B2222BB", outcome = "POSITIVE", deleted = LocalDateTime.now() to "Recorded in error"),
          scanEntity(prisonerNumber, outcome = "POSITIVE", typeOfFind = "INORGANIC"),
          scanEntity(prisonerNumber, scanDate = startOfYear.minusDays(1), outcome = "POSITIVE", typeOfFind = "INORGANIC"),
        ),
      )

      val summary = scanRepository.scanSummaryRowsForPrisoners(
        listOf(prisonerNumber, "B2222BB"),
        startOfYear,
        today,
        includeDeleted,
      ).groupOutcomes()

      if (includeDeleted) {
        assertThat(summary).isEqualTo(
          mapOf(
            prisonerNumber to mapOf(
              "POSITIVE" to 2,
              "NEGATIVE" to 1,
            ),
            "B2222BB" to mapOf(
              "POSITIVE" to 1,
              "INCONCLUSIVE" to 1,
            ),
          ),
        )
      } else {
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
  }

  private fun scanEntity(
    prisonerNumber: String = this.prisonerNumber,
    scanDate: LocalDate = this.scanDate,
    justification: String = "REASONABLE_SUSPICION",
    outcome: String = "NEGATIVE",
    typeOfFind: String? = null,
    deleted: Pair<LocalDateTime, String>? = null,
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
  ).apply {
    deletedAt = deleted?.first
    deletedReason = deleted?.second
  }
}
