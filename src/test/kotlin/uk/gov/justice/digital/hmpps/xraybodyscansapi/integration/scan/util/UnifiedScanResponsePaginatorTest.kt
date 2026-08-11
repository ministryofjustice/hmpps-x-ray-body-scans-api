package uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.scan.util

import com.fasterxml.uuid.Generators
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.FixedClock
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.response.LegacyScanResponse
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.response.ScanResponse
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.response.UnifiedScanResponse
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.util.UnifiedScanResponsePaginator
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.util.UUID

class UnifiedScanResponsePaginatorTest {
  companion object : FixedClock()

  @Test
  fun `merges two empty sequences`() {
    val dpsSequence = emptyScanSequence()
    val nomisSequence = emptyScanSequence()

    val page = dpsSequence.paginateWith(nomisSequence, pageRequest(0, 20))

    assertThat(page).isEmpty()
    assertThat(page.number).isEqualTo(0)
    assertThat(page.size).isEqualTo(20)
    assertThat(page.totalElements).isEqualTo(0)
    assertThat(page.totalPages).isEqualTo(0)
    assertThat(page.numberOfElements).isEqualTo(0)
    assertThat(page.content).isEqualTo(emptyList<UnifiedScanResponse>())
    assertThat(page.pageable.isPaged).isTrue()
    assertThat(page.pageable.sort.isSorted).isTrue()
  }

  @Test
  fun `merges a sequence with an empty one`() {
    val (dpsSequence, dpsScans) = dpsScansForDaysIntoThePast(2)
    val nomisSequence = emptyScanSequence()

    val page = dpsSequence.paginateWith(nomisSequence, pageRequest(0, 20))

    assertThat(page.number).isEqualTo(0)
    assertThat(page.size).isEqualTo(20)
    assertThat(page.totalElements).isEqualTo(2)
    assertThat(page.totalPages).isEqualTo(1)
    assertThat(page.numberOfElements).isEqualTo(2)
    assertThat(page.content).isEqualTo(dpsScans)
    assertThat(page.content).allMatch { !it.isLegacy }
  }

  @Test
  fun `merges an empty sequence with another one`() {
    val dpsSequence = emptyScanSequence()
    val (nomisSequence, nomisScans) = nomisScansForDaysIntoThePast(2)

    val page = dpsSequence.paginateWith(nomisSequence, pageRequest(0, 20))

    assertThat(page.number).isEqualTo(0)
    assertThat(page.size).isEqualTo(20)
    assertThat(page.totalElements).isEqualTo(2)
    assertThat(page.totalPages).isEqualTo(1)
    assertThat(page.numberOfElements).isEqualTo(2)
    assertThat(page.content).isEqualTo(nomisScans)
    assertThat(page.content).allMatch { it.isLegacy }
  }

  @Test
  fun `merges two sequences`() {
    val (dpsSequence) = dpsScansForDaysIntoThePast(2)
    val (nomisSequence) = nomisScansForDaysIntoThePast(3)

    val page = dpsSequence.paginateWith(nomisSequence, pageRequest(0, 5))

    assertThat(page.number).isEqualTo(0)
    assertThat(page.size).isEqualTo(5)
    assertThat(page.totalElements).isEqualTo(5)
    assertThat(page.totalPages).isEqualTo(1)
    assertThat(page.numberOfElements).isEqualTo(5)
    assertThat(page.content.take(2)).allSatisfy {
      assertThat(it.scanDate).isEqualTo(today.minusDays(1))
    }
    assertThat(page.content.drop(2).take(2)).allSatisfy {
      assertThat(it.scanDate).isEqualTo(today.minusDays(2))
    }
    assertThat(page.content.drop(4)).allSatisfy {
      assertThat(it.scanDate).isEqualTo(today.minusDays(3))
      assertThat(it.isLegacy).isTrue()
    }
  }

  @Test
  fun `merges two sequences according to comparator`() {
    val (dpsSequence) = dpsScansForDaysIntoThePast(5, offset = 6)
    val (nomisSequence) = nomisScansForDaysIntoThePast(5)

    val page = dpsSequence.paginateWith(nomisSequence, pageRequest(1, 5))

    assertThat(page.number).isEqualTo(1)
    assertThat(page.size).isEqualTo(5)
    assertThat(page.totalElements).isEqualTo(10)
    assertThat(page.totalPages).isEqualTo(2)
    assertThat(page.numberOfElements).isEqualTo(5)
    assertThat(page.content).allMatch { !it.isLegacy }
  }

  @ParameterizedTest(name = "returns empty page {0} when beyond both sequences")
  @ValueSource(ints = [1, 2])
  fun `returns empty page when beyond both sequences`(pageNumber: Int) {
    val (dpsSequence) = dpsScansForDaysIntoThePast(2)
    val (nomisSequence) = nomisScansForDaysIntoThePast(3)

    val page = dpsSequence.paginateWith(nomisSequence, pageRequest(pageNumber, 5))

    assertThat(page).isEmpty()
    assertThat(page.number).isEqualTo(pageNumber)
    assertThat(page.size).isEqualTo(5)
    assertThat(page.totalElements).isEqualTo(5)
    assertThat(page.totalPages).isEqualTo(1)
    assertThat(page.numberOfElements).isEqualTo(0)
  }

  @Test
  fun `returns page 1 from a sequence merged with an empty one`() {
    val (dpsSequence) = dpsScansForDaysIntoThePast(5)
    val nomisSequence = emptyScanSequence()

    val page = dpsSequence.paginateWith(nomisSequence, pageRequest(1, 4))

    assertThat(page.number).isEqualTo(1)
    assertThat(page.size).isEqualTo(4)
    assertThat(page.totalElements).isEqualTo(5)
    assertThat(page.totalPages).isEqualTo(2)
    assertThat(page.numberOfElements).isEqualTo(1)
    assertThat(page.content).hasSize(1)
    val scan = page.content[0]
    assertThat(scan.isLegacy).isFalse()
    assertThat(scan.scanDate).isEqualTo(today.minusDays(5))
  }

  @Test
  fun `returns page 1 from an empty sequence merged with another one`() {
    val dpsSequence = emptyScanSequence()
    val (nomisSequence) = nomisScansForDaysIntoThePast(5)

    val page = dpsSequence.paginateWith(nomisSequence, pageRequest(1, 4))

    assertThat(page.number).isEqualTo(1)
    assertThat(page.size).isEqualTo(4)
    assertThat(page.totalElements).isEqualTo(5)
    assertThat(page.totalPages).isEqualTo(2)
    assertThat(page.numberOfElements).isEqualTo(1)
    assertThat(page.content).hasSize(1)
    val scan = page.content[0]
    assertThat(scan.isLegacy).isTrue()
    assertThat(scan.scanDate).isEqualTo(today.minusDays(5))
  }

  @Test
  fun `returns page 1 from two merged sequences`() {
    val (dpsSequence) = dpsScansForDaysIntoThePast(5)
    val (nomisSequence) = nomisScansForDaysIntoThePast(5)

    val page = dpsSequence.paginateWith(nomisSequence, pageRequest(1, 8))

    assertThat(page.number).isEqualTo(1)
    assertThat(page.size).isEqualTo(8)
    assertThat(page.totalElements).isEqualTo(10)
    assertThat(page.totalPages).isEqualTo(2)
    assertThat(page.numberOfElements).isEqualTo(2)
    assertThat(page.content).hasSize(2)
    val (scan1, scan2) = page.content
    assertThat(scan1.isLegacy).isFalse()
    assertThat(scan1.scanDate).isEqualTo(today.minusDays(5))
    assertThat(scan2.isLegacy).isTrue()
    assertThat(scan2.scanDate).isEqualTo(today.minusDays(5))
  }

  @DisplayName("Lazy evaluation of sequences")
  @Nested
  inner class Lazy {
    @Test
    fun `only peeks at the next element in the sequence when needed`() {
      val dpsSequence = UnifiedScanResponsePaginator(
        4,
        dpsScansForDaysIntoThePast(3).first.sequence + generateSequence {
          fail("4th DPS scan should not be retrieved")
        },
      )
      val nomisSequence = UnifiedScanResponsePaginator(
        3,
        nomisScansForDaysIntoThePast(2, offset = 2).first.sequence + generateSequence {
          fail("3rd NOMIS scan should not be retrieved")
        },
      )

      val page = dpsSequence.paginateWith(nomisSequence, pageRequest(0, 4))

      assertThat(page.number).isEqualTo(0)
      assertThat(page.size).isEqualTo(4)
      assertThat(page.totalElements).isEqualTo(7)
      assertThat(page.totalPages).isEqualTo(2)
      assertThat(page.numberOfElements).isEqualTo(4)
      assertThat(page.content).hasSize(4)
    }

    @Test
    fun `returns for infinite sequences`() {
      val id = UUID.fromString("019fc832-57b9-704f-a907-8059720e37e8")
      val dpsSequence = UnifiedScanResponsePaginator(
        10_000,
        generateSequence {
          dpsScanResponse(id, today)
        },
      )
      val nomisSequence = UnifiedScanResponsePaginator(
        10_000,
        generateSequence {
          nomisScanResponse(74521, today)
        },
      )

      val page = dpsSequence.paginateWith(nomisSequence, pageRequest(2, 5))

      assertThat(page.number).isEqualTo(2)
      assertThat(page.size).isEqualTo(5)
      assertThat(page.totalElements).isEqualTo(20_000)
      assertThat(page.totalPages).isEqualTo(4_000)
      assertThat(page.numberOfElements).isEqualTo(5)
      assertThat(page.content).hasSize(5)
      assertThat(page.content).allMatch { !it.isLegacy }
    }
  }

  // all comparison is via separately-tested `SortComparator` so can test paginating with one constant option
  private val sort = Sort.by(Sort.Direction.DESC, "scanDate")
    .and(Sort.by(Sort.Direction.ASC, "source"))
  private fun pageRequest(pageNumber: Int = 0, pageSize: Int = 20): Pageable = PageRequest.of(pageNumber, pageSize, sort)

  private fun dpsScanResponse(originalId: UUID, scanDate: LocalDate) = ScanResponse(
    originalId = originalId,
    prisonerNumber = "A1234BC",
    prisonId = "MDI",
    scanDate = scanDate,
    justification = "INTELLIGENCE",
    justificationDescription = "Intelligence",
    outcome = "NEGATIVE",
    outcomeDescription = "Negative",
    typeOfFind = null,
    typeOfFindDescription = null,
    createdAt = now,
    createdBy = "abc12ab",
    lastModifiedAt = now,
    lastModifiedBy = "abc12ab",
  )

  private fun nomisScanResponse(originalId: Long, scanDate: LocalDate) = LegacyScanResponse(
    originalId = originalId,
    prisonerNumber = "A1234BC",
    scanDate = scanDate,
    scanDetails = "object detected",
  )

  private val uuidGenerator = Generators.timeBasedGenerator()
  private fun uuid(scanDate: LocalDate) = uuidGenerator.construct(scanDate.toEpochSecond(LocalTime.NOON, ZoneOffset.UTC))

  private fun dpsScansForDaysIntoThePast(days: Long, offset: Long = 1): Pair<UnifiedScanResponsePaginator<ScanResponse>, List<ScanResponse>> {
    val scans = (offset..<offset + days).map { daysIntoPast ->
      val scanDate = today.minusDays(daysIntoPast)
      dpsScanResponse(uuid(scanDate), scanDate)
    }
    return UnifiedScanResponsePaginator(days.toInt(), scans.asSequence()) to scans
  }

  private fun nomisScansForDaysIntoThePast(days: Long, offset: Long = 1): Pair<UnifiedScanResponsePaginator<LegacyScanResponse>, List<LegacyScanResponse>> {
    val scans = (offset..<offset + days).map { daysIntoPast ->
      val scanDate = today.minusDays(daysIntoPast)
      nomisScanResponse(days - daysIntoPast, scanDate)
    }
    return UnifiedScanResponsePaginator(days.toInt(), scans.asSequence()) to scans
  }

  private fun emptyScanSequence() = UnifiedScanResponsePaginator(0, emptySequence())
}
