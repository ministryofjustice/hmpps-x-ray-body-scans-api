package uk.gov.justice.digital.hmpps.xraybodyscansapi.client.prisonapi.response

import org.springframework.data.domain.Sort
import uk.gov.justice.digital.hmpps.xraybodyscansapi.util.SortComparator
import kotlin.reflect.KProperty1

class PersonalCareNeedComparator(sort: Sort) : SortComparator<PersonalCareNeed>(sort) {
  override fun mapToProperty(name: String): KProperty1<PersonalCareNeed, Comparable<*>?> = when (name) {
    "id" -> PersonalCareNeed::personalCareNeedId
    "scanDate" -> PersonalCareNeed::startDate
    else -> throw IllegalArgumentException("cannot sort care needs by $name")
  }
}
