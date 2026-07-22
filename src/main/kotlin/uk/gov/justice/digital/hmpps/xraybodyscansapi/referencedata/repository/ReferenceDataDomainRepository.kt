package uk.gov.justice.digital.hmpps.xraybodyscansapi.referencedata.repository

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ReferenceDataDomainRepository : JpaRepository<ReferenceDataDomainEntity, String> {
  @EntityGraph(attributePaths = ["codes"])
  override fun findAll(): List<ReferenceDataDomainEntity>

  @EntityGraph(attributePaths = ["codes"])
  fun findByCode(code: String): ReferenceDataDomainEntity?
}
