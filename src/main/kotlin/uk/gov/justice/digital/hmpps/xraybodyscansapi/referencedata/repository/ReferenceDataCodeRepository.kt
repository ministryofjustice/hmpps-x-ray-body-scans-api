package uk.gov.justice.digital.hmpps.xraybodyscansapi.referencedata.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.xraybodyscansapi.referencedata.dto.response.ReferenceDataDomains

@Repository
interface ReferenceDataCodeRepository : JpaRepository<ReferenceDataCodeEntity, Int> {
  @Deprecated("Use friendly-named findByDomain")
  @Suppress("ktlint:standard:function-naming")
  fun findByDomain_CodeOrderByListSequence(domain: String): List<ReferenceDataCodeEntity>

  /** Find all pre-sorted codes in a domain (domain would be loaded lazily) */
  fun findByDomain(domain: String): List<ReferenceDataCodeEntity> = findByDomain_CodeOrderByListSequence(domain)

  @Deprecated("Use friendly-named findByDomainAndCode")
  @Suppress("ktlint:standard:function-naming")
  fun findByDomain_CodeAndCode(domain: String, code: String): ReferenceDataCodeEntity?

  /** Find a code in a domain (domain would be loaded lazily) */
  fun findByDomainAndCode(domain: String, code: String): ReferenceDataCodeEntity? = findByDomain_CodeAndCode(domain, code)

  /** Find a code in a domain (domain would be loaded lazily) */
  fun findByDomainAndCode(domain: ReferenceDataDomains, code: String): ReferenceDataCodeEntity? = findByDomain_CodeAndCode(domain.name, code)
}
