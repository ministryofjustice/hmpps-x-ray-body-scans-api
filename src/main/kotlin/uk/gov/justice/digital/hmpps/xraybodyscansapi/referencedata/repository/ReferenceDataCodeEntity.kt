package uk.gov.justice.digital.hmpps.xraybodyscansapi.referencedata.repository

import jakarta.persistence.Column
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapsId
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "reference_data_code")
class ReferenceDataCodeEntity(
  @EmbeddedId
  val id: ReferenceDataKey,

  @Column(name = "description", nullable = false)
  val description: String,

  @Suppress("unused")
  @Column(name = "list_sequence", nullable = false)
  val listSequence: Int,

  @Column(name = "created_by", nullable = false, updatable = false)
  val createdBy: String,
  @Column(name = "last_modified_by")
  val lastModifiedBy: String,
) {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @MapsId("domain")
  @JoinColumn(name = "domain", nullable = false, updatable = false)
  lateinit var domain: ReferenceDataDomainEntity

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
}
