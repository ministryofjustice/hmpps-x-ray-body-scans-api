package uk.gov.justice.digital.hmpps.xraybodyscansapi.referencedata.repository

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ReferenceDataDomainRepository : JpaRepository<ReferenceDataDomainEntity, String> {
  /** Find all domains and eagerly load all pre-sorted codes */
  @EntityGraph(attributePaths = ["codes"])
  override fun findAll(): List<ReferenceDataDomainEntity>

  /** Find a domain and eagerly load all its pre-sorted codes */
  @EntityGraph(attributePaths = ["codes"])
  fun findByCode(code: String): ReferenceDataDomainEntity?
}
