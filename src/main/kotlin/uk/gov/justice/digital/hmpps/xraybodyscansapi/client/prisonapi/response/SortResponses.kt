package uk.gov.justice.digital.hmpps.xraybodyscansapi.client.prisonapi.response

import org.springframework.data.domain.Sort
import kotlin.reflect.KProperty1

private fun mapToCareNeedProperty(name: String): KProperty1<PersonalCareNeed, Comparable<*>?> = when (name) {
  "id" -> PersonalCareNeed::personalCareNeedId
  "scanDate" -> PersonalCareNeed::startDate
  else -> throw NotImplementedError("cannot sort care needs by $name")
}

/**
 * Sorts list of personal care needs in place using Spring Data domain object.
 * NB:
 * - ignore case is not supported, always case sensitive
 * - null handling is always nulls last
 */
fun MutableList<PersonalCareNeed>.sortWith(sort: Sort) {
  val sortOrders = sort.map { sortOrder ->
    val property = mapToCareNeedProperty(sortOrder.property)
    val direction = sortOrder.direction
    property to direction
  }.toList()
  sortWith { careNeed1, careNeed2 ->
    sortOrders.forEach { (property, direction) ->
      val property1 = property.get(careNeed1)
      val property2 = property.get(careNeed2)
      if (property1 == null) {
        return@sortWith 1
      }
      if (property2 == null) {
        return@sortWith -1
      }
      @Suppress("UNCHECKED_CAST") // we know the two types are the same and comparable
      val order = (property1 as Comparable<Any>).compareTo(property2)
      if (order != 0) {
        return@sortWith if (direction == Sort.Direction.DESC) {
          -order
        } else {
          order
        }
      }
    }
    0 // equal for all orders
  }
}
