package uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.scan.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.repository.ScanEntity
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.repository.ScanRepository
import java.time.LocalDate
import java.time.LocalDateTime

@DataJpaTest
@ActiveProfiles("test")
class ScanRepositoryTest {

  @Autowired
  private lateinit var scanRepository: ScanRepository

  private val prisonerNumber = "A1234BC"
  private val scanDate: LocalDate = LocalDate.now().minusDays(1)

  @Test
  fun `save persists a scan correctly`() {
    val before = LocalDateTime.now().minusSeconds(10)
    val after = LocalDateTime.now().plusSeconds(10)

    val saved = scanRepository.save(
      ScanEntity(prisonerNumber = prisonerNumber, scanDate = scanDate),
    )
    assertThat(saved.id).isGreaterThan(0L)
    assertThat(saved.createdAt).isNotNull()
    assertThat(saved.createdAt).isBetween(before, after)

    val found = scanRepository.findById(saved.id).orElseThrow()
    assertThat(found.prisonerNumber).isEqualTo(prisonerNumber)
    assertThat(found.scanDate).isEqualTo(scanDate)
  }
}
