package uk.gov.justice.digital.hmpps.xraybodyscansapi.referencedata.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.xraybodyscansapi.referencedata.dto.response.ReferenceDataCode
import uk.gov.justice.digital.hmpps.xraybodyscansapi.referencedata.dto.response.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.xraybodyscansapi.referencedata.repository.ReferenceDataCodeRepository
import uk.gov.justice.digital.hmpps.xraybodyscansapi.referencedata.repository.ReferenceDataDomainRepository

@Service
@Transactional(readOnly = true)
class ReferenceDataService(
  private val domainRepository: ReferenceDataDomainRepository,
  private val codeRepository: ReferenceDataCodeRepository,
) {
  fun getAllReferenceData(): Map<String, ReferenceDataDomain> = domainRepository.findAll()
    .associate { it.code to ReferenceDataDomain(it) }

  @Suppress("unused")
  fun getReferenceDataDomain(domain: String): ReferenceDataDomain? = domainRepository.findByCode(domain)
    ?.let { ReferenceDataDomain(it) }

  @Suppress("unused")
  fun getReferenceDataDomainList(domain: String): List<ReferenceDataCode> = codeRepository.findByDomain(domain)
    .map { ReferenceDataCode(it) }

  @Suppress("unused")
  fun getReferenceData(domain: String, code: String): ReferenceDataCode? = codeRepository.findByDomainAndCode(domain, code)
    ?.let { ReferenceDataCode(it) }
}
