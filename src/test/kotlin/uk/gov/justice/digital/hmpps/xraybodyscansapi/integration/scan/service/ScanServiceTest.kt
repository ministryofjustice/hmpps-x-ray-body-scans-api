package uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.scan.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.xraybodyscansapi.client.prisonapi.PrisonApiClient
import uk.gov.justice.digital.hmpps.xraybodyscansapi.client.prisonapi.response.PersonalCareNeed
import uk.gov.justice.digital.hmpps.xraybodyscansapi.client.prisonapi.response.PersonalCareNeedsResponse
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.request.CreateScanRequest
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.response.ScanCountResponse
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.repository.ScanEntity
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.repository.ScanRepository
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.service.ScanService
import java.time.LocalDate

class ScanServiceTest {

  private val scanRepository = mock<ScanRepository>()
  private val prisonApiClient = mock<PrisonApiClient>()
  private val scanService = ScanService(scanRepository, prisonApiClient)

  @Nested
  inner class Create {

    private val prisonerNumber = "A1234BC"
    private val scanDate: LocalDate = LocalDate.now().minusDays(1)

    @Test
    fun `persists a scan entity built from the request and returns response built from the saved entity`() {
      whenever(scanRepository.save(any<ScanEntity>())).thenAnswer { invocation ->
        invocation.getArgument<ScanEntity>(0)
      }

      val response = scanService.createScan(prisonerNumber, CreateScanRequest(scanDate = scanDate))

      val captor = argumentCaptor<ScanEntity>()
      verify(scanRepository).save(captor.capture())
      assertThat(captor.firstValue.prisonerNumber).isEqualTo(prisonerNumber)
      assertThat(captor.firstValue.scanDate).isEqualTo(scanDate)

      assertThat(response.prisonerNumber).isEqualTo(prisonerNumber)
      assertThat(response.scanDate).isEqualTo(scanDate)
    }
  }

  @Nested
  inner class CountScans {

    private val fromScanDate: LocalDate = LocalDate.parse("2026-01-01")
    private val toScanDate: LocalDate = LocalDate.parse("2026-02-01")

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
      whenever(scanRepository.findByPrisonerNumberInAndScanDateBetween(prisonerNumbers, fromScanDate, toScanDate))
        .thenReturn(
          listOf(
            scanEntity("A1234BC"),
            scanEntity("A1234BC"),
            scanEntity("A1234BC"),
            scanEntity("B1234AC"),
          ),
        )

      val result = scanService.countScans(prisonerNumbers, fromScanDate, toScanDate)

      assertThat(result).containsExactly(
        ScanCountResponse(prisonerNumber = "A1234BC", nomisCount = 2, dpsCount = 3, totalCount = 5),
        ScanCountResponse(prisonerNumber = "B1234AC", nomisCount = 1, dpsCount = 1, totalCount = 2),
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
      whenever(scanRepository.findByPrisonerNumberInAndScanDateBetween(listOf(prisonerNumber), fromScanDate, toScanDate))
        .thenReturn(listOf(scanEntity("A1234BC"), scanEntity("A1234BC"), scanEntity("A1234BC")))

      val result = scanService.countScans(prisonerNumber, fromScanDate, toScanDate)

      assertThat(result).isEqualTo(
        ScanCountResponse(prisonerNumber = "A1234BC", nomisCount = 2, dpsCount = 3, totalCount = 5),
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
      whenever(scanRepository.findByPrisonerNumberInAndScanDateBetween(prisonerNumbers, fromScanDate, toScanDate))
        .thenReturn(listOf(scanEntity("B1234AC"), scanEntity("B1234AC")))

      val result = scanService.countScans(prisonerNumbers, fromScanDate, toScanDate)

      assertThat(result).containsExactly(
        ScanCountResponse(prisonerNumber = "A1234BC", nomisCount = 4, dpsCount = 0, totalCount = 4),
        ScanCountResponse(prisonerNumber = "B1234AC", nomisCount = 0, dpsCount = 2, totalCount = 2),
        ScanCountResponse(prisonerNumber = "C1234AB", nomisCount = 0, dpsCount = 0, totalCount = 0),
      )
    }

    private fun bscan(startDate: String) = PersonalCareNeed(
      personalCareNeedId = 1,
      problemType = "BSCAN",
      problemCode = "xyz",
      problemStatus = "xyz",
      startDate = LocalDate.parse(startDate),
    )

    private fun scanEntity(prisonerNumber: String) = ScanEntity(
      prisonerNumber = prisonerNumber,
      scanDate = LocalDate.now().minusDays(1),
    )
  }
}
