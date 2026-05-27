package uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.scan.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.request.CreateScanRequest
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.repository.ScanEntity
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.repository.ScanRepository
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.service.ScanService
import java.time.LocalDate

class ScanServiceTest {

  private val scanRepository: ScanRepository = mock()
  private val scanService = ScanService(scanRepository)

  @Nested
  inner class Create {

    private val prisonerNumber = "A1234BC"
    private val scanDate: LocalDate = LocalDate.now().minusDays(1)

    @Test
    fun `persists a scan entity built from the request and returns response built from the saved entity`() {
      whenever(scanRepository.save(any<ScanEntity>())).thenAnswer { invocation ->
        val entity = invocation.getArgument<ScanEntity>(0)
        ScanEntity(
          prisonerNumber = entity.prisonerNumber,
          scanDate = entity.scanDate,
          id = 42L,
        )
      }

      val response = scanService.createScan(prisonerNumber, CreateScanRequest(scanDate = scanDate))

      val captor = argumentCaptor<ScanEntity>()
      verify(scanRepository).save(captor.capture())
      assertThat(captor.firstValue.prisonerNumber).isEqualTo(prisonerNumber)
      assertThat(captor.firstValue.scanDate).isEqualTo(scanDate)
      assertThat(captor.firstValue.id).isNull()

      assertThat(response.id).isEqualTo(42L)
      assertThat(response.prisonerNumber).isEqualTo(prisonerNumber)
      assertThat(response.scanDate).isEqualTo(scanDate)
    }
  }
}
