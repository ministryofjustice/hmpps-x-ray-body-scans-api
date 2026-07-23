package uk.gov.justice.digital.hmpps.xraybodyscansapi.referencedata.repository

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "reference_data_domain")
class ReferenceDataDomainEntity(
  @Id
  @Column(name = "code", nullable = false, updatable = false)
  val code: String,
  @Column(name = "description", nullable = false)
  val description: String,

  @Column(name = "created_by", nullable = false, updatable = false)
  val createdBy: String,
  @Column(name = "last_modified_by", nullable = false)
  val lastModifiedBy: String,
) {
  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  val createdAt: LocalDateTime = LocalDateTime.now()

  @UpdateTimestamp
  @Column(name = "last_modified_at", nullable = false)
  val lastModifiedAt: LocalDateTime = LocalDateTime.now()

  @Column(name = "deactivated_at")
  val deactivatedAt: LocalDateTime? = null

  @Column(name = "deactivated_by")
  val deactivatedBy: String? = null

  @OneToMany(mappedBy = "domainCode", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  @OrderBy("listSequence ASC")
  val codes: MutableList<ReferenceDataCodeEntity> = mutableListOf()
}
