package uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.service

import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.xraybodyscansapi.client.casenotes.CaseNotesApiClient
import uk.gov.justice.digital.hmpps.xraybodyscansapi.client.casenotes.response.CaseNoteResponse
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
  private val caseNotesApiClient: CaseNotesApiClient,
) : HmppsPrisonSubjectAccessRequestService {

  @Transactional(readOnly = true)
  override fun getPrisonContentFor(
    prn: String,
    fromDate: LocalDate?,
    toDate: LocalDate?,
  ): HmppsSubjectAccessRequestContent? {
    var spec = filterByPrisonerNumber(prn)
    fromDate?.let { spec = spec.and(filterFromScanDate(it)) }
    toDate?.let { spec = spec.and(filterToScanDate(it)) }

    val scans = scanRepository.findAll(spec, Sort.by("scanDate").descending())

    // We should replace this longer term with the case notes search endpoint by the XRBS type
    val caseNotes = scans.filter { s -> s.caseNoteId != null }.map { s -> s.caseNoteId!! }
      .map { caseNotesApiClient.getCaseNote(prn, it.toString()) }

    val mappedScans = scans.map { scan ->
      val caseNote = caseNotes.find { it.caseNoteId == scan.caseNoteId.toString() }
      SarScanResponse(
        person = scan.prisonerNumber,
        date = scan.scanDate,
        justification = scan.justification.description,
        outcome = scan.outcome.description,
        find = scan.typeOfFind?.description,
        establishment = scan.prisonId,
        additionalDetails = if (caseNote != null) {
          caseNoteToSarText(caseNote)
        } else {
          null
        },
      )
    }

    return if (mappedScans.isEmpty()) null else HmppsSubjectAccessRequestContent(content = mappedScans)
  }

  companion object {
    fun caseNoteToSarText(caseNote: CaseNoteResponse): String {
      val output = mutableListOf(
        caseNote.text,
      )
      caseNote.amendments.forEach {
        output.add("\nMore details added:\n")
        output.add(it.additionalNoteText)
      }

      return output.joinToString("\n")
    }
  }
}
