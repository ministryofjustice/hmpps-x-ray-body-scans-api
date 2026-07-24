package uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.repository

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import uk.gov.justice.digital.hmpps.xraybodyscansapi.jpa.GeneratedUuidV7
import uk.gov.justice.digital.hmpps.xraybodyscansapi.referencedata.repository.ReferenceDataCodeEntity
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "body_scan")
class ScanEntity(
  @Column(name = "prisoner_number", nullable = false)
  val prisonerNumber: String,

  @Column(name = "prison_id", nullable = false)
  val prisonId: String,

  @Column(name = "scan_date", nullable = false)
  val scanDate: LocalDate,

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "justification", nullable = false)
  val justification: ReferenceDataCodeEntity,
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "outcome", nullable = false)
  val outcome: ReferenceDataCodeEntity,
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "type_of_find")
  val typeOfFind: ReferenceDataCodeEntity? = null,

  @Column(name = "case_note_id")
  val caseNoteId: UUID? = null,

  @Column(name = "created_by", nullable = false, updatable = false)
  val createdBy: String,
  @Column(name = "last_modified_by")
  val lastModifiedBy: String = createdBy,
) {
  @Id
  @GeneratedUuidV7
  @Column(name = "id", nullable = false, updatable = false)
  lateinit var id: UUID

  @Column(name = "outcome", nullable = false, insertable = false, updatable = false)
  var justificationCode: String = ""

  @Column(name = "outcome", nullable = false, insertable = false, updatable = false)
  var outcomeCode: String = ""

  @Column(name = "outcome", insertable = false, updatable = false)
  var typeOfFindCode: String? = null

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  val createdAt: LocalDateTime = LocalDateTime.now()

  @UpdateTimestamp
  @Column(name = "last_modified_at", nullable = false)
  val lastModifiedAt: LocalDateTime = LocalDateTime.now()
}
