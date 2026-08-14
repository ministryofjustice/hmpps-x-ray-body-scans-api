package uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.service

import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.sar.SarScanResponse
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.repository.ScanRepository
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.repository.filterByPrisonerNumber
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.repository.filterFromScanDate
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.repository.filterToScanDate
import uk.gov.justice.hmpps.kotlin.sar.HmppsPrisonSubjectAccessRequestService
import uk.gov.justice.hmpps.kotlin.sar.HmppsSubjectAccessRequestContent
import java.time.LocalDate

@Service
class SubjectAccessRequestService(
  private val scanRepository: ScanRepository,
) : HmppsPrisonSubjectAccessRequestService {

  @Transactional(readOnly = true)
  override fun getPrisonContentFor(prn: String, fromDate: LocalDate?, toDate: LocalDate?): HmppsSubjectAccessRequestContent? {
    var spec = filterByPrisonerNumber(prn)
    fromDate?.let { spec = spec.and(filterFromScanDate(it)) }
    toDate?.let { spec = spec.and(filterToScanDate(it)) }

    val scans = scanRepository.findAll(spec, Sort.by("scanDate").descending())
      .map { scan ->
        SarScanResponse(
          person = scan.prisonerNumber,
          date = scan.scanDate,
          justification = scan.justification.description,
          outcome = scan.outcome.description,
          find = scan.typeOfFind?.description,
          establishment = scan.prisonId,
        )
      }

    return if (scans.isEmpty()) null else HmppsSubjectAccessRequestContent(content = scans)
  }
}
