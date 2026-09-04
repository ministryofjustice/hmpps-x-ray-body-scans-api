package uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.scan.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatList
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import uk.gov.justice.digital.hmpps.xraybodyscansapi.client.alertsapi.AlertsApiClient
import uk.gov.justice.digital.hmpps.xraybodyscansapi.client.alertsapi.response.Alert
import uk.gov.justice.digital.hmpps.xraybodyscansapi.client.alertsapi.response.AlertCode
import uk.gov.justice.digital.hmpps.xraybodyscansapi.client.alertsapi.response.AlertResponse
import uk.gov.justice.digital.hmpps.xraybodyscansapi.client.casenotes.CaseNotesApiClient
import uk.gov.justice.digital.hmpps.xraybodyscansapi.client.casenotes.request.CreateCaseNoteRequest
import uk.gov.justice.digital.hmpps.xraybodyscansapi.client.casenotes.response.CaseNoteResponse
import uk.gov.justice.digital.hmpps.xraybodyscansapi.client.prisonapi.PrisonApiClient
import uk.gov.justice.digital.hmpps.xraybodyscansapi.client.prisonapi.response.PersonalCareNeed
import uk.gov.justice.digital.hmpps.xraybodyscansapi.client.prisonapi.response.PersonalCareNeedsResponse
import uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.FixedClock
import uk.gov.justice.digital.hmpps.xraybodyscansapi.referencedata.dto.response.ReferenceDataDomains
import uk.gov.justice.digital.hmpps.xraybodyscansapi.referencedata.repository.ReferenceDataCodeEntity
import uk.gov.justice.digital.hmpps.xraybodyscansapi.referencedata.repository.ReferenceDataCodeRepository
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.Source
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.request.CreateScanCaseNoteRequest
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.request.CreateScanRequest
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.request.ListScansRequest
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.response.LegacyScanResponse
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.response.ScanResponse
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.response.ScanSummaryResponse
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.repository.ScanEntity
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.repository.ScanRepository
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.repository.ScanSummaryRow
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.service.IncludeAlerts
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.service.ScanService
import java.time.LocalDate
import java.util.UUID
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.response.AlertResponse as AlertResponseDto

class ScanServiceTest {
  companion object : FixedClock()

  private val relevantAlertCodes = setOf("XIS", "XXRAY")

  private val codeRepository = mock<ReferenceDataCodeRepository>()
  private val scanRepository = mock<ScanRepository>()
  private val prisonApiClient = mock<PrisonApiClient>()
  private val alertsApiClient = mock<AlertsApiClient>()
  private val caseNotesApiClient = mock<CaseNotesApiClient>()
  private val scanService = ScanService(
    clock,
    codeRepository,
    scanRepository,
    prisonApiClient,
    alertsApiClient,
    caseNotesApiClient,
    scanAnnualLimit = 116,
    nearingLimitThreshold = 100,
    relevantAlertCodes = relevantAlertCodes,
  )

  @DisplayName("Get DPS scans")
  @Nested
  inner class Get {
    private val ids = MutableList(3) { UUID.randomUUID() }

    @Test
    fun `returns empty list when no scans are found`() {
      whenever(scanRepository.findByIdIn(ids))
        .thenReturn(emptyList())

      val scans = scanService.getScans(ids)
      assertThat(scans).isEmpty()
      verifyNoInteractions(prisonApiClient)
    }

    @Test
    fun `returns list of scans`() {
      whenever(scanRepository.findByIdIn(ids))
        .thenReturn(
          listOf(
            scanEntity("A1111AA"),
            scanEntity("B2222BB"),
          ),
        )

      val scans = scanService.getScans(ids)
      assertThat(scans).hasSize(2)
      assertThat(scans.map { it.prisonerNumber to it.source }).containsExactly(
        "A1111AA" to Source.DPS,
        "B2222BB" to Source.DPS,
      )
      verifyNoInteractions(prisonApiClient)
    }
  }

  @DisplayName("Listing DPS and legacy NOMIS scans")
  @Nested
  inner class List {
    private val prisonerNumber = "A1234BC"

    @Test
    fun `returns empty list when there are no scans for a prisoner`() {
      whenever(
        scanRepository.findAll(
          any<Specification<ScanEntity>>(),
          eq(PageRequest.of(0, 20, Sort.by("scanDate", "id").descending())),
        ),
      ).thenReturn(PageImpl(emptyList()))
      whenever(prisonApiClient.getScanCareNeeds(listOf(prisonerNumber)))
        .thenReturn(emptyList())

      val scans = scanService.listScans(prisonerNumber)
      assertThat(scans).isEmpty()
    }

    @Test
    fun `returns a page of 20 DPS scans for a prisoner`() {
      whenever(
        scanRepository.findAll(
          any<Specification<ScanEntity>>(),
          eq(PageRequest.of(0, 20, Sort.by("scanDate", "id").descending())),
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
      whenever(
        scanRepository.findAll(
          any<Specification<ScanEntity>>(),
          eq(PageRequest.of(1, 20, Sort.by("scanDate", "id").descending())),
        ),
      ).thenThrow(AssertionError("should not access page 1"))
      whenever(prisonApiClient.getScanCareNeeds(listOf(prisonerNumber)))
        .thenReturn(emptyList())

      val scans = scanService.listScans(prisonerNumber)
      assertThat(scans).hasSize(3)
      assertThatList(scans.content).allMatch {
        it.prisonerNumber == prisonerNumber && it.source == Source.DPS
      }
    }

    @Test
    fun `returns a page of 20 NOMIS scans for a prisoner`() {
      whenever(
        scanRepository.findAll(
          any<Specification<ScanEntity>>(),
          eq(PageRequest.of(0, 20, Sort.by("scanDate", "id").descending())),
        ),
      ).thenReturn(PageImpl(emptyList()))
      whenever(
        scanRepository.findAll(
          any<Specification<ScanEntity>>(),
          eq(PageRequest.of(1, 20, Sort.by("scanDate", "id").descending())),
        ),
      ).thenThrow(AssertionError("should not access page 1"))
      whenever(prisonApiClient.getScanCareNeeds(listOf(prisonerNumber)))
        .thenReturn(
          listOf(
            PersonalCareNeedsResponse(
              offenderNo = prisonerNumber,
              personalCareNeeds = listOf(
                bscan(today),
                bscan(today.minusDays(4)),
                bscan(today.minusMonths(1)),
                bscan(today.minusMonths(2)),
              ),
            ),
          ),
        )

      val scans = scanService.listScans(prisonerNumber)
      assertThat(scans).hasSize(4)
      assertThatList(scans.content).allMatch {
        it.prisonerNumber == prisonerNumber && it.source == Source.NOMIS
      }
    }

    @Test
    fun `returns pages of DPS scans as specified`() {
      whenever(
        scanRepository.findAll(
          any<Specification<ScanEntity>>(),
          eq(PageRequest.of(0, 10, Sort.by("scanDate", "id").ascending())),
        ),
      ).thenReturn(
        PageImpl(
          MutableList(10) {
            scanEntity(prisonerNumber, justification = "REASONABLE_SUSPICION")
          },
        ),
      )
      whenever(
        scanRepository.findAll(
          any<Specification<ScanEntity>>(),
          eq(PageRequest.of(1, 10, Sort.by("scanDate", "id").ascending())),
        ),
      ).thenReturn(
        PageImpl(
          listOf(
            scanEntity(prisonerNumber, justification = "INTELLIGENCE"),
          ),
        ),
      )
      whenever(
        scanRepository.findAll(
          any<Specification<ScanEntity>>(),
          eq(PageRequest.of(2, 10, Sort.by("scanDate", "id").ascending())),
        ),
      ).thenThrow(AssertionError("should not access page 2"))
      whenever(prisonApiClient.getScanCareNeeds(listOf(prisonerNumber)))
        .thenReturn(emptyList())

      val scans =
        scanService.listScans(prisonerNumber, pageable = PageRequest.of(1, 10, Sort.by("scanDate").ascending()))
      assertThat(scans).hasSize(1)
      val dpsScan = scans.content[0] as ScanResponse
      assertThat(dpsScan.justification).isEqualTo("INTELLIGENCE")
    }

    @Test
    fun `returns pages of DPS and NOMIS scans as specified`() {
      whenever(
        scanRepository.findAll(
          any<Specification<ScanEntity>>(),
          eq(PageRequest.of(0, 10, Sort.by("scanDate", "id").descending())),
        ),
      ).thenReturn(
        PageImpl(
          MutableList(10) {
            scanEntity(prisonerNumber, justification = "REASONABLE_SUSPICION")
          },
        ),
      )
      whenever(
        scanRepository.findAll(
          any<Specification<ScanEntity>>(),
          eq(PageRequest.of(1, 10, Sort.by("scanDate", "id").descending())),
        ),
      ).thenReturn(
        PageImpl(
          listOf(
            scanEntity(prisonerNumber, justification = "INTELLIGENCE"),
          ),
        ),
      )
      whenever(prisonApiClient.getScanCareNeeds(listOf(prisonerNumber)))
        .thenReturn(
          listOf(
            PersonalCareNeedsResponse(
              offenderNo = prisonerNumber,
              personalCareNeeds = listOf(bscan(today.minusWeeks(2))),
            ),
          ),
        )

      val scans = scanService.listScans(prisonerNumber, pageable = PageRequest.of(1, 10))
      assertThat(scans).hasSize(2)
      val dpsScan = scans.content[0] as ScanResponse
      assertThat(dpsScan.justification).isEqualTo("INTELLIGENCE")
      val nomisScan = scans.content[1] as LegacyScanResponse
      assertThat(nomisScan.scanDetails).isEqualTo("notes")
    }

    @DisplayName("allows huge page sizes when date filters span a year or less")
    @TestFactory
    fun `allows huge page sizes when date filters span a year or less`() = listOf(
      "with open-ended date filters" to ListScansRequest(fromScanDate = yearStart),
      "with date filters spanning a year" to ListScansRequest(
        fromScanDate = yearStart.minusYears(1),
        toScanDate = yearStart,
      ),
    ).map {
      val (scenario, query) = it
      DynamicTest.dynamicTest(scenario) {
        whenever(
          scanRepository.findAll(any<Specification<ScanEntity>>(), any<Pageable>()),
        ).thenReturn(PageImpl(emptyList()))
        whenever(
          prisonApiClient.getScanCareNeeds(any()),
        ).thenReturn(emptyList())

        val scans = scanService.listScans(prisonerNumber, query, PageRequest.of(0, 10_000, Sort.by("scanDate")))
        assertThat(scans).isEmpty()
      }
    }

    @DisplayName("throws validation error when attempting to retrieve too many scans")
    @TestFactory
    fun `throws validation error when attempting to retrieve too many scans`() = listOf(
      "without filters" to null,
      "with no date filters" to ListScansRequest(),
      "with open-ended date filters" to ListScansRequest(toScanDate = today.minusDays(1)),
      "with date filters spanning more than a year" to ListScansRequest(fromScanDate = yearStart.minusMonths(8)),
    ).map {
      val (scenario, query) = it
      DynamicTest.dynamicTest(scenario) {
        assertThatThrownBy {
          scanService.listScans(prisonerNumber, query, PageRequest.of(0, 201, Sort.by("scanDate")))
        }.hasMessage("Page size limit of 200 exceeded")
        verifyNoInteractions(prisonApiClient)
        verifyNoInteractions(scanRepository)
      }
    }

    @Test
    fun `throws validation error when attempting to retrieve no scans`() {
      assertThatThrownBy {
        scanService.listScans(prisonerNumber, null, PageRequest.of(0, 0, Sort.by("scanDate")))
      }.hasMessage("Page size must not be less than one")
      verifyNoInteractions(prisonApiClient)
      verifyNoInteractions(scanRepository)
    }

    @Test
    fun `throws validation error when attempting to sort by an invalid field`() {
      assertThatThrownBy {
        scanService.listScans(prisonerNumber, null, PageRequest.of(0, 20, Sort.by("id")))
      }.hasMessage("Sort order not supported (only [scanDate] are allowed)")
      verifyNoInteractions(prisonApiClient)
      verifyNoInteractions(scanRepository)
    }
  }

  @DisplayName("Recording a new scan")
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
      verifyNoInteractions(scanRepository)
    }

    @Test
    fun `throws validation error when outcome is positive but no type of find is provided`() {
      makeReferenceDataWheneverNeeded()
      val request = CreateScanRequest(
        scanDate = scanDate,
        prisonId = "MDI",
        justification = "INTELLIGENCE",
        outcome = "POSITIVE",
        typeOfFind = null,
        createdBy = "abc12a",
      )
      assertThatThrownBy {
        scanService.createScan(prisonerNumber, request)
      }.hasMessage("typeOfFind is required for positive outcomes")
      verifyNoInteractions(scanRepository)
    }
  }

  @DisplayName("Creating a case note for a scan")
  @Nested
  inner class CreateCaseNote {
    private val prisonerNumber = "A1234BC"
    private val scanId = UUID.randomUUID()
    private val caseNoteId = UUID.randomUUID()

    @Test
    fun `creates case note and saves its id to the scan`() {
      val scan = scanEntity(prisonerNumber).apply { id = scanId }
      val occurredAt = scan.scanDate.atStartOfDay()
      whenever(scanRepository.findById(scanId)).thenReturn(java.util.Optional.of(scan))
      whenever(caseNotesApiClient.createCaseNote(eq(prisonerNumber), any<CreateCaseNoteRequest>()))
        .thenReturn(
          CaseNoteResponse(
            caseNoteId = caseNoteId.toString(),
            offenderIdentifier = prisonerNumber,
            type = "GEN",
            typeDescription = "General",
            subType = "XRBS",
            subTypeDescription = "X-Ray Body Scan",
            text = "some text",
            creationDateTime = now,
            occurrenceDateTime = occurredAt,
            authorName = "John Smith",
            amendments = emptyList(),
          ),
        )
      whenever(scanRepository.save(any<ScanEntity>())).thenAnswer { it.getArgument(0) }

      val response = scanService.createCaseNote(scanId, CreateScanCaseNoteRequest(text = "some text"))

      val caseNoteCaptor = argumentCaptor<CreateCaseNoteRequest>()
      verify(caseNotesApiClient).createCaseNote(eq(prisonerNumber), caseNoteCaptor.capture())
      assertThat(caseNoteCaptor.firstValue.type).isEqualTo("GEN")
      assertThat(caseNoteCaptor.firstValue.subType).isEqualTo("XRBS")
      assertThat(caseNoteCaptor.firstValue.text).isEqualTo("some text")
      assertThat(caseNoteCaptor.firstValue.occurrenceDateTime).isEqualTo(occurredAt)

      val scanCaptor = argumentCaptor<ScanEntity>()
      verify(scanRepository).save(scanCaptor.capture())
      assertThat(scanCaptor.firstValue.caseNoteId).isEqualTo(caseNoteId)

      assertThat(response.id).isEqualTo(caseNoteId.toString())
      assertThat(response.title).isEqualTo("X-Ray Body Scan")
      assertThat(response.text).isEqualTo("some text")
      assertThat(response.createdBy).isEqualTo("John Smith")
      assertThat(response.createdAt).isEqualTo(now)
      assertThat(response.occurredAt).isEqualTo(occurredAt)
    }

    @Test
    fun `throws entity not found exception when scan is not found`() {
      whenever(scanRepository.findById(scanId)).thenReturn(java.util.Optional.empty())

      assertThatThrownBy {
        scanService.createCaseNote(scanId, CreateScanCaseNoteRequest(text = "some text"))
      }.hasMessage("Scan with id $scanId not found")

      verifyNoInteractions(caseNotesApiClient)
    }
  }

  @DisplayName("Summarising scans")
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
      whenever(scanRepository.scanSummaryRowsForPrisoners(prisonerNumbers, yearStart, today))
        .thenReturn(
          listOf(
            scanSummaryRow("B1234AC"),
            scanSummaryRow("A1234BC", count = 3),
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
          nearingScanLimit = false,
          atScanLimit = false,
          relevantAlerts = null,
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
          nearingScanLimit = false,
          atScanLimit = false,
          relevantAlerts = null,
          fromScanDate = yearStart,
          toScanDate = today,
        ),
      )
      verifyNoInteractions(alertsApiClient)
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
      whenever(scanRepository.scanSummaryRowsForPrisoners(listOf(prisonerNumber), yearStart, today))
        .thenReturn(listOf(scanSummaryRow("A1234BC", count = 3)))

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
          nearingScanLimit = false,
          atScanLimit = false,
          relevantAlerts = null,
          fromScanDate = yearStart,
          toScanDate = today,
        ),
      )
      verifyNoInteractions(alertsApiClient)
    }

    @Test
    fun `defaults missing counts to zero`() {
      val prisonerNumbers = listOf("A1234BC", "B1234AC", "C1234AB")

      whenever(prisonApiClient.getScanCareNeeds(prisonerNumbers))
        .thenReturn(
          listOf(
            PersonalCareNeedsResponse(
              offenderNo = "A1234BC",
              personalCareNeeds = listOf(
                bscan("2026-01-10"),
                bscan("2026-01-02"),
                bscan("2026-01-03"),
                bscan("2026-01-04"),
              ),
            ),
          ),
        )
      whenever(scanRepository.scanSummaryRowsForPrisoners(prisonerNumbers, yearStart, today))
        .thenReturn(listOf(scanSummaryRow("B1234AC", count = 2)))

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
          nearingScanLimit = false,
          atScanLimit = false,
          relevantAlerts = null,
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
          nearingScanLimit = false,
          atScanLimit = false,
          relevantAlerts = null,
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
          nearingScanLimit = false,
          atScanLimit = false,
          relevantAlerts = null,
          fromScanDate = yearStart,
          toScanDate = today,
        ),
      )
      verifyNoInteractions(alertsApiClient)
    }

    @Test
    fun `counts positive, negative and inconclusive DPS scans`() {
      val prisonerNumber = "A1234BC"

      whenever(prisonApiClient.getScanCareNeeds(listOf(prisonerNumber)))
        .thenReturn(listOf(PersonalCareNeedsResponse(offenderNo = prisonerNumber)))
      whenever(scanRepository.scanSummaryRowsForPrisoners(listOf(prisonerNumber), yearStart, today))
        .thenReturn(
          listOf(
            scanSummaryRow(prisonerNumber, outcome = "POSITIVE"),
            scanSummaryRow(prisonerNumber, outcome = "NEGATIVE", count = 2),
            scanSummaryRow(prisonerNumber, outcome = "INCONCLUSIVE"),
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
      whenever(scanRepository.scanSummaryRowsForPrisoners(listOf(prisonerNumber), yearStart, today))
        .thenReturn(listOf(scanSummaryRow(prisonerNumber, count = 5)))

      val result = scanService.summariseScans(prisonerNumber)

      assertThat(result.totalCount).isEqualTo(5)
      assertThat(result.remainingScans).isEqualTo(111)
    }

    @Test
    fun `flags prisoners who are nearing or have reached the annual limit`() {
      // A1234BC is over limit
      // B1234AC is at limit
      // C1234AB is over nearing limit threshold, but below limit
      // D1234FG is at nearing limit threshold
      // E1234HI is below nearing limit threshold

      val prisonerNumbers = listOf("A1234BC", "B1234AC", "C1234AB", "D1234FG", "E1234HI")
      whenever(prisonApiClient.getScanCareNeeds(prisonerNumbers))
        .thenReturn(
          listOf(
            // A1234BC has 50 nomis scans
            PersonalCareNeedsResponse(
              offenderNo = "A1234BC",
              personalCareNeeds = (1L..50).map {
                bscan(yearStart.plusDays(it))
              },
            ),
            // B1234AC, C1234AB and D1234FG have 0 nomis scans
            PersonalCareNeedsResponse(offenderNo = "B1234AC"),
            PersonalCareNeedsResponse(offenderNo = "C1234AB"),
            PersonalCareNeedsResponse(offenderNo = "D1234FG"),
            // E1234HI has no nomis scans
          ),
        )

      whenever(scanRepository.scanSummaryRowsForPrisoners(prisonerNumbers, yearStart, today))
        .thenReturn(
          listOf(
            // A1234BC has 67 dps scans
            scanSummaryRow("A1234BC", count = 67),
            // B1234AC has 116 dps scans
            scanSummaryRow("B1234AC", count = 116),
            // C1234AB has 101 dps scans
            scanSummaryRow("C1234AB", count = 101),
            // D1234FG has 100 dps scans
            scanSummaryRow("D1234FG", count = 100),
            // E1234HI has no dps scans
          ),
        )

      val result = scanService.summariseScans(prisonerNumbers)

      data class SimplifiedSummary(
        val prisonerNumber: String,
        val totalCount: Int,
        val remainingScans: Int,
        val nearingScanLimit: Boolean,
        val atScanLimit: Boolean,
      )

      val simpleResult = result.map {
        SimplifiedSummary(
          prisonerNumber = it.prisonerNumber,
          totalCount = it.totalCount,
          remainingScans = it.remainingScans,
          nearingScanLimit = it.nearingScanLimit,
          atScanLimit = it.atScanLimit,
        )
      }

      assertThat(simpleResult).isEqualTo(
        listOf(
          SimplifiedSummary("A1234BC", 117, -1, true, true),
          SimplifiedSummary("B1234AC", 116, 0, true, true),
          SimplifiedSummary("C1234AB", 101, 15, true, false),
          SimplifiedSummary("D1234FG", 100, 16, true, false),
          SimplifiedSummary("E1234HI", 0, 116, false, false),
        ),
      )
    }

    @DisplayName("Relevant alerts in scan summaries")
    @Nested
    inner class Alerts {
      val prisonerNumbers = listOf("A1234AA", "B1234BB")

      @BeforeEach
      fun setup() {
        // alerts do not interact with actual scan data, so can say there were none
        whenever(prisonApiClient.getScanCareNeeds(prisonerNumbers))
          .thenReturn(emptyList())
        whenever(scanRepository.scanSummaryRowsForPrisoners(prisonerNumbers, yearStart, today))
          .thenReturn(emptyList())
      }

      @Test
      fun `returns alerts when requested`() {
        whenever(alertsApiClient.getAlerts(prisonerNumbers, relevantAlertCodes, "abc12a"))
          .thenReturn(
            AlertResponse(
              listOf(
                alert("A1234AA", "X", "XXRAY", id = "019fcc21-8aaf-75a8-9c27-ec1e006fe35e"),
                alert("B1234BB", "X", "XIS", id = "019fcc21-8df6-7278-8869-8fe992a46c68"),
              ),
            ),
          )
        val result = scanService.summariseScans(prisonerNumbers, IncludeAlerts.WithUsername("abc12a"))

        val relevantAlerts = result.associate { it.prisonerNumber to it.relevantAlerts }
        assertThat(relevantAlerts).isEqualTo(
          mapOf(
            "A1234AA" to listOf(
              AlertResponseDto(
                id = "019fcc21-8aaf-75a8-9c27-ec1e006fe35e",
                type = "X",
                typeDescription = "X",
                code = "XXRAY",
                codeDescription = "XXRAY",
              ),
            ),
            "B1234BB" to listOf(
              AlertResponseDto(
                id = "019fcc21-8df6-7278-8869-8fe992a46c68",
                type = "X",
                typeDescription = "X",
                code = "XIS",
                codeDescription = "XIS",
              ),
            ),
          ),
        )
      }

      @Test
      fun `returns empty list if no relevant codes`() {
        whenever(alertsApiClient.getAlerts(prisonerNumbers, relevantAlertCodes, "user3"))
          .thenReturn(AlertResponse(emptyList()))
        val result = scanService.summariseScans(prisonerNumbers, IncludeAlerts.WithUsername("user3"))

        val relevantAlerts = result.associate { it.prisonerNumber to it.relevantAlerts }
        assertThat(relevantAlerts).isEqualTo(
          mapOf(
            "A1234AA" to emptyList<AlertResponseDto>(),
            "B1234BB" to emptyList(),
          ),
        )
      }

      @Test
      @Disabled("filtering is delegated to alerts-api")
      fun `filters alerts`() {
        whenever(alertsApiClient.getAlerts(prisonerNumbers, relevantAlertCodes, "abc12a"))
          .thenReturn(
            AlertResponse(
              listOf(
                alert("A1234AA", "L", "LCE"),
              ),
            ),
          )
        val result = scanService.summariseScans(prisonerNumbers, IncludeAlerts.WithUsername("abc12a"))

        val relevantAlerts = result.associate { it.prisonerNumber to it.relevantAlerts }
        assertThat(relevantAlerts).isEqualTo(
          mapOf(
            "A1234AA" to emptyList<AlertResponseDto>(),
            "B1234BB" to emptyList(),
          ),
        )
      }
    }

    private fun alert(
      prisonerNumber: String,
      type: String,
      code: String,
      id: String = "019fc832-57b9-704f-a907-8059720e37e8",
    ) = Alert(
      alertUuid = id,
      prisonNumber = prisonerNumber,
      alertCode = AlertCode(
        alertTypeCode = type,
        alertTypeDescription = type,
        code = code,
        description = code,
      ),
      description = "",
    )
  }

  private fun bscan(startDate: String) = bscan(LocalDate.parse(startDate))
  private fun bscan(startDate: LocalDate) = PersonalCareNeed(
    personalCareNeedId = 1,
    problemType = "BSCAN",
    problemCode = "BSC6.0",
    problemStatus = "ON",
    problemDescription = "Body Scan (6.0 µSv)",
    commentText = "notes",
    startDate = startDate,
  )

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

  private fun scanSummaryRow(
    prisonerNumber: String,
    outcome: String = "NEGATIVE",
    count: Int = 1,
  ) = object : ScanSummaryRow {
    override val prisonerNumber: String = prisonerNumber
    override val outcome: String = outcome
    override val count: Int = count
  }
}
