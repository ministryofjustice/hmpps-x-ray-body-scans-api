package uk.gov.justice.digital.hmpps.xraybodyscansapi.referencedata.repository

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.io.Serializable

@Embeddable
data class ReferenceDataKey(
  @Column(name = "domain", nullable = false)
  val domain: String = "",
  @Column(name = "code", nullable = false)
  val code: String = "",
) : Serializable
