package uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.scan.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatList
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import uk.gov.justice.digital.hmpps.xraybodyscansapi.client.prisonapi.PrisonApiClient
import uk.gov.justice.digital.hmpps.xraybodyscansapi.client.prisonapi.response.PersonalCareNeed
import uk.gov.justice.digital.hmpps.xraybodyscansapi.client.prisonapi.response.PersonalCareNeedsResponse
import uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.FixedClock
import uk.gov.justice.digital.hmpps.xraybodyscansapi.referencedata.dto.response.ReferenceDataDomains
import uk.gov.justice.digital.hmpps.xraybodyscansapi.referencedata.repository.ReferenceDataCodeEntity
import uk.gov.justice.digital.hmpps.xraybodyscansapi.referencedata.repository.ReferenceDataCodeRepository
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.request.CreateScanRequest
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.response.ScanSummaryResponse
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.repository.ScanEntity
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.repository.ScanRepository
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.service.ScanService
import java.time.LocalDate
import java.util.UUID

class ScanServiceTest {
  companion object : FixedClock()

  private val codeRepository = mock<ReferenceDataCodeRepository>()
  private val scanRepository = mock<ScanRepository>()
  private val prisonApiClient = mock<PrisonApiClient>()
  private val scanService = ScanService(clock, codeRepository, scanRepository, prisonApiClient, scanAnnualLimit = 116)

  @Nested
  inner class List {
    private val prisonerNumber = "A1234BC"

    @Test
    fun `returns a page of 20 scans for a prisoner`() {
      whenever(
        scanRepository.findAll(
          any<Specification<ScanEntity>>(),
          eq(PageRequest.of(0, 20, Sort.by("scanDate").descending())),
        ),
      ).thenReturn(
        PageImpl(
          listOf(
            scanEntity(prisonerNumber),
            scanEntity(prisonerNumber),
            scanEntity(prisonerNumber),
          ),
        ),
      )

      val scans = scanService.listScans(prisonerNumber)
      assertThat(scans).hasSize(3)
      assertThatList(scans.content).allMatch { it.prisonerNumber == prisonerNumber }
    }

    @Test
    fun `returns pages of scans as specified`() {
      whenever(
        scanRepository.findAll(
          any<Specification<ScanEntity>>(),
          eq(PageRequest.of(1, 10, Sort.by("id").ascending())),
        ),
      ).thenReturn(PageImpl(listOf(scanEntity(prisonerNumber))))

      val scans = scanService.listScans(prisonerNumber, pageable = PageRequest.of(1, 10, Sort.by("id").ascending()))
      assertThat(scans).hasSize(1)
    }
  }

  @Nested
  inner class Create {

    private val prisonerNumber = "A1234BC"
    private val scanDate: LocalDate = today.minusDays(1)

    @Test
    fun `persists a scan entity built from the request and returns response built from the saved entity`() {
      makeReferenceDataWheneverNeeded()
      whenever(scanRepository.save(any<ScanEntity>())).thenAnswer { invocation ->
        val scanEntity = invocation.getArgument<ScanEntity>(0)
        scanEntity.apply { id = UUID.randomUUID() }
      }

      val response = scanService.createScan(
        prisonerNumber,
        CreateScanRequest(
          scanDate = scanDate,
          prisonId = "MDI",
          justification = "INTELLIGENCE",
          outcome = "NEGATIVE",
          createdBy = "abc12a",
        ),
      )

      val captor = argumentCaptor<ScanEntity>()
      verify(scanRepository).save(captor.capture())
      assertThat(captor.firstValue.prisonerNumber).isEqualTo(prisonerNumber)
      assertThat(captor.firstValue.scanDate).isEqualTo(scanDate)

      assertThat(response.prisonerNumber).isEqualTo(prisonerNumber)
      assertThat(response.scanDate).isEqualTo(scanDate)
      assertThat(response.justification).isEqualTo("INTELLIGENCE")
      assertThat(response.outcome).isEqualTo("NEGATIVE")
      assertThat(response.typeOfFind).isNull()
      assertThat(response.lastModifiedBy).isEqualTo("abc12a")
    }

    @ParameterizedTest(name = "throws validation error when requested {0} is invalid")
    @EnumSource(ReferenceDataDomains::class)
    fun `throws validation error when reference data is invalid`(domain: ReferenceDataDomains) {
      makeReferenceDataWheneverNeeded(setOf(domain.name to "INVALID"))
      val request = CreateScanRequest(
        scanDate = scanDate,
        prisonId = "MDI",
        justification = if (domain == ReferenceDataDomains.JUSTIFICATION) "INVALID" else "INTELLIGENCE",
        outcome = if (domain == ReferenceDataDomains.OUTCOME) "INVALID" else "POSITIVE",
        typeOfFind = if (domain == ReferenceDataDomains.TYPE_OF_FIND) "INVALID" else "INORGANIC",
        createdBy = "abc12a",
      )
      assertThatThrownBy {
        scanService.createScan(prisonerNumber, request)
      }.hasMessage("Reference data with domain ${domain.name} and code INVALID not found")
    }
  }

  @Nested
  inner class Summarise {
    @Test
    fun `returns correct counts for a list of prisoners, filtering nomis scans by date range`() {
      val prisonerNumbers = listOf("A1234BC", "B1234AC")

      whenever(prisonApiClient.getScanCareNeeds(prisonerNumbers))
        .thenReturn(
          listOf(
            PersonalCareNeedsResponse(
              offenderNo = "A1234BC",
              personalCareNeeds = listOf(
                bscan("2026-01-01"),
                bscan("2026-01-02"),
                bscan("2025-12-31"),
              ),
            ),
            PersonalCareNeedsResponse(
              offenderNo = "B1234AC",
              personalCareNeeds = listOf(bscan("2026-01-03")),
            ),
          ),
        )
      whenever(scanRepository.findByPrisonerNumberInAndScanDateBetween(prisonerNumbers, yearStart, today))
        .thenReturn(
          listOf(
            scanEntity("A1234BC"),
            scanEntity("A1234BC"),
            scanEntity("A1234BC"),
            scanEntity("B1234AC"),
          ),
        )

      val result = scanService.summariseScans(prisonerNumbers)

      assertThat(result).containsExactly(
        ScanSummaryResponse(
          prisonerNumber = "A1234BC",
          nomisCount = 2,
          dpsCount = 3,
          totalCount = 5,
          positiveCount = 0,
          negativeCount = 3,
          inconclusiveCount = 0,
          annualLimit = 116,
          remainingScans = 111,
          fromScanDate = yearStart,
          toScanDate = today,
        ),
        ScanSummaryResponse(
          prisonerNumber = "B1234AC",
          nomisCount = 1,
          dpsCount = 1,
          totalCount = 2,
          positiveCount = 0,
          negativeCount = 1,
          inconclusiveCount = 0,
          annualLimit = 116,
          remainingScans = 114,
          fromScanDate = yearStart,
          toScanDate = today,
        ),
      )
    }

    @Test
    fun `returns count for a single prisoner`() {
      val prisonerNumber = "A1234BC"

      whenever(prisonApiClient.getScanCareNeeds(listOf(prisonerNumber)))
        .thenReturn(
          listOf(
            PersonalCareNeedsResponse(
              offenderNo = "A1234BC",
              personalCareNeeds = listOf(bscan("2026-01-01"), bscan("2026-01-02")),
            ),
          ),
        )
      whenever(scanRepository.findByPrisonerNumberInAndScanDateBetween(listOf(prisonerNumber), yearStart, today))
        .thenReturn(listOf(scanEntity("A1234BC"), scanEntity("A1234BC"), scanEntity("A1234BC")))

      val result = scanService.summariseScans(prisonerNumber)

      assertThat(result).isEqualTo(
        ScanSummaryResponse(
          prisonerNumber = "A1234BC",
          nomisCount = 2,
          dpsCount = 3,
          totalCount = 5,
          positiveCount = 0,
          negativeCount = 3,
          inconclusiveCount = 0,
          annualLimit = 116,
          remainingScans = 111,
          fromScanDate = yearStart,
          toScanDate = today,
        ),
      )
    }

    @Test
    fun `defaults missing counts to zero`() {
      val prisonerNumbers = listOf("A1234BC", "B1234AC", "C1234AB")

      whenever(prisonApiClient.getScanCareNeeds(prisonerNumbers))
        .thenReturn(
          listOf(
            PersonalCareNeedsResponse(
              offenderNo = "A1234BC",
              personalCareNeeds = listOf(bscan("2026-01-10"), bscan("2026-01-02"), bscan("2026-01-03"), bscan("2026-01-04")),
            ),
          ),
        )
      whenever(scanRepository.findByPrisonerNumberInAndScanDateBetween(prisonerNumbers, yearStart, today))
        .thenReturn(listOf(scanEntity("B1234AC"), scanEntity("B1234AC")))

      val result = scanService.summariseScans(prisonerNumbers)

      assertThat(result).containsExactly(
        ScanSummaryResponse(
          prisonerNumber = "A1234BC",
          nomisCount = 4,
          dpsCount = 0,
          totalCount = 4,
          positiveCount = 0,
          negativeCount = 0,
          inconclusiveCount = 0,
          annualLimit = 116,
          remainingScans = 112,
          fromScanDate = yearStart,
          toScanDate = today,
        ),
        ScanSummaryResponse(
          prisonerNumber = "B1234AC",
          nomisCount = 0,
          dpsCount = 2,
          totalCount = 2,
          positiveCount = 0,
          negativeCount = 2,
          inconclusiveCount = 0,
          annualLimit = 116,
          remainingScans = 114,
          fromScanDate = yearStart,
          toScanDate = today,
        ),
        ScanSummaryResponse(
          prisonerNumber = "C1234AB",
          nomisCount = 0,
          dpsCount = 0,
          totalCount = 0,
          positiveCount = 0,
          negativeCount = 0,
          inconclusiveCount = 0,
          annualLimit = 116,
          remainingScans = 116,
          fromScanDate = yearStart,
          toScanDate = today,
        ),
      )
    }

    @Test
    fun `counts positive, negative and inconclusive DPS scans`() {
      val prisonerNumber = "A1234BC"

      whenever(prisonApiClient.getScanCareNeeds(listOf(prisonerNumber)))
        .thenReturn(listOf(PersonalCareNeedsResponse(offenderNo = prisonerNumber)))
      whenever(scanRepository.findByPrisonerNumberInAndScanDateBetween(listOf(prisonerNumber), yearStart, today))
        .thenReturn(
          listOf(
            scanEntity(prisonerNumber, outcome = "POSITIVE"),
            scanEntity(prisonerNumber, outcome = "NEGATIVE"),
            scanEntity(prisonerNumber, outcome = "NEGATIVE"),
            scanEntity(prisonerNumber, outcome = "INCONCLUSIVE"),
          ),
        )

      val result = scanService.summariseScans(prisonerNumber)

      assertThat(result.positiveCount).isEqualTo(1)
      assertThat(result.negativeCount).isEqualTo(2)
      assertThat(result.inconclusiveCount).isEqualTo(1)
      assertThat(result.dpsCount).isEqualTo(4)
    }

    @Test
    fun `calculates remaining scans as annual limit minus total scans`() {
      val prisonerNumber = "A1234BC"

      whenever(prisonApiClient.getScanCareNeeds(listOf(prisonerNumber)))
        .thenReturn(listOf(PersonalCareNeedsResponse(offenderNo = prisonerNumber)))
      whenever(scanRepository.findByPrisonerNumberInAndScanDateBetween(listOf(prisonerNumber), yearStart, today))
        .thenReturn(
          listOf(
            scanEntity(prisonerNumber),
            scanEntity(prisonerNumber),
            scanEntity(prisonerNumber),
            scanEntity(prisonerNumber),
            scanEntity(prisonerNumber),
          ),
        )

      val result = scanService.summariseScans(prisonerNumber)

      assertThat(result.totalCount).isEqualTo(5)
      assertThat(result.remainingScans).isEqualTo(111)
    }

    private fun bscan(startDate: String) = PersonalCareNeed(
      personalCareNeedId = 1,
      problemType = "BSCAN",
      problemCode = "xyz",
      problemStatus = "xyz",
      startDate = LocalDate.parse(startDate),
    )
  }

  private fun makeReferenceDataWheneverNeeded(missingCodes: Set<Pair<String, String>> = emptySet()) {
    whenever(codeRepository.findByDomainAndCode(any<ReferenceDataDomains>(), any<String>())).thenAnswer { invocation ->
      val domain = invocation.getArgument(0) as ReferenceDataDomains
      val code = invocation.getArgument(1) as String
      if (missingCodes.contains(domain.name to code)) {
        null
      } else {
        referenceData(domain = domain, code = code)
      }
    }
  }

  private fun referenceData(
    domain: ReferenceDataDomains,
    code: String,
  ) = ReferenceDataCodeEntity(
    domainCode = domain.name,
    code = code,
    description = code,
    listSequence = 0,
    createdBy = "CONNECT_DPS",
  ).apply {
    // updates to entity that would be done by jpa/hibernate
    id = (1..100).random()
  }

  private fun scanEntity(
    prisonerNumber: String,
    prisonId: String = "MDI",
    justification: String = "INTELLIGENCE",
    outcome: String = "NEGATIVE",
    typeOfFind: String? = null,
    createdBy: String = "abc12ab",
  ) = ScanEntity(
    prisonerNumber = prisonerNumber,
    prisonId = prisonId,
    scanDate = today.minusDays(1),
    justification = referenceData(ReferenceDataDomains.JUSTIFICATION, justification),
    outcome = referenceData(ReferenceDataDomains.OUTCOME, outcome),
    typeOfFind = typeOfFind?.let { referenceData(ReferenceDataDomains.TYPE_OF_FIND, typeOfFind) },
    createdBy = createdBy,
  ).apply {
    // updates to entity that would be done by jpa/hibernate
    id = UUID.randomUUID()
  }
}
