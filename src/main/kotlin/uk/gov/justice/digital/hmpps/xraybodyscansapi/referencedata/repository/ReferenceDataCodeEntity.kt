package uk.gov.justice.digital.hmpps.xraybodyscansapi.referencedata.repository

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "reference_data_code")
class ReferenceDataCodeEntity(
  @Column(name = "domain", nullable = false)
  val domainCode: String,

  @Column(name = "code", nullable = false)
  val code: String,

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
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false, updatable = false)
  var id: Int = 0

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "domain", nullable = false, insertable = false, updatable = false)
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
