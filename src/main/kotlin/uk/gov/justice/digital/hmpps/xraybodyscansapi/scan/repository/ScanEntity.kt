package uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.repository

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "scans")
class ScanEntity(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false, updatable = false)
  val id: Long? = null,

  @Column(name = "prisoner_number", nullable = false)
  val prisonerNumber: String,

  @Column(name = "scan_date", nullable = false)
  val scanDate: LocalDate,

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  val createdAt: LocalDateTime? = null,
)
