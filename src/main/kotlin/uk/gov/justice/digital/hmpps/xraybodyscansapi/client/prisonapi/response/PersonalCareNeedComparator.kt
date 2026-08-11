package uk.gov.justice.digital.hmpps.xraybodyscansapi.client.prisonapi.response

import org.springframework.data.domain.Sort
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.Source
import uk.gov.justice.digital.hmpps.xraybodyscansapi.util.SortComparator

class PersonalCareNeedComparator(sort: Sort) : SortComparator<PersonalCareNeed>(sort) {
  override fun getterForProperty(name: String): (PersonalCareNeed) -> Comparable<*>? = when (name) {
    "id" -> PersonalCareNeed::personalCareNeedId
    "scanDate" -> PersonalCareNeed::startDate
    "source" -> { _ -> Source.NOMIS }
    else -> throw IllegalArgumentException("cannot sort care needs by $name")
  }
}
