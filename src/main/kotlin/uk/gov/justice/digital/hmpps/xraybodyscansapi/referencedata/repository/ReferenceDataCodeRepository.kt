package uk.gov.justice.digital.hmpps.xraybodyscansapi.referencedata.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
interface ReferenceDataCodeRepository : JpaRepository<ReferenceDataCodeEntity, ReferenceDataKey> {
  @Suppress("ktlint:standard:function-naming")
  fun findByDomain_CodeOrderByListSequence(domain: String): List<ReferenceDataCodeEntity>

  @Suppress("unused")
  fun findByDomain(domain: String) = findByDomain_CodeOrderByListSequence(domain)

  @Suppress("unused")
  fun findByDomainAndCode(domain: String, code: String): ReferenceDataCodeEntity? = findByIdOrNull(ReferenceDataKey(domain, code))
}
