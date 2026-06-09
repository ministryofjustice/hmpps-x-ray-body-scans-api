package uk.gov.justice.digital.hmpps.xraybodyscansapi.client.prisonapi.response

import java.time.LocalDate

data class PersonalCareNeedsResponse(
  val offenderNo: String,
  val personalCareNeeds: List<PersonalCareNeed> = emptyList(),
)

data class PersonalCareNeed(
  val personalCareNeedId: Long,
  val problemType: String,
  val problemCode: String,
  val problemStatus: String,
  val problemDescription: String? = null,
  val commentText: String? = null,
  val startDate: LocalDate? = null,
  val endDate: LocalDate? = null,
)
