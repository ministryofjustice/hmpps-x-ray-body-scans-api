package uk.gov.justice.digital.hmpps.xraybodyscansapi.util

import org.springframework.data.domain.Sort
import kotlin.reflect.KProperty1

/**
 * Sorts objects using Spring Data domain object.
 * NB:
 * - ignore case is not supported, always case sensitive
 * - null handling is always nulls last
 */
abstract class SortComparator<T>(sort: Sort) : Comparator<T> {
  val sortOrders = sort.map { sortOrder ->
    val property = mapToProperty(sortOrder.property)
    val direction = sortOrder.direction
    property to direction
  }.toList()

  abstract fun mapToProperty(name: String): KProperty1<T, Comparable<*>?>

  override fun compare(object1: T, object2: T): Int {
    sortOrders.forEach { (property, direction) ->
      val property1 = property.get(object1)
      val property2 = property.get(object2)
      if (property1 == null) {
        return 1
      }
      if (property2 == null) {
        return -1
      }
      @Suppress("UNCHECKED_CAST") // we know the two types are the same and comparable
      val order = (property1 as Comparable<Any>).compareTo(property2)
      if (order != 0) {
        return if (direction == Sort.Direction.DESC) {
          -order
        } else {
          order
        }
      }
    }

    return 0 // equal for all orders
  }
}
