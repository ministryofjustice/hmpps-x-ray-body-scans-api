package uk.gov.justice.digital.hmpps.xraybodyscansapi.util

import org.springframework.data.domain.Sort

/**
 * Sorts objects using Spring Data domain object.
 * NB:
 * - ignore case is not supported, always case sensitive
 * - null handling is always nulls last
 */
abstract class SortComparator<T>(sort: Sort) : Comparator<T> {
  val sortOrders = sort.map { sortOrder ->
    val getter = getterForProperty(sortOrder.property)
    val direction = sortOrder.direction
    getter to direction
  }.toList()

  abstract fun getterForProperty(name: String): (T) -> Comparable<*>?

  override fun compare(object1: T, object2: T): Int {
    sortOrders.forEach { (getter, direction) ->
      val value1 = getter(object1)
      val value2 = getter(object2)
      if (value1 == null) {
        return 1
      }
      if (value2 == null) {
        return -1
      }
      @Suppress("UNCHECKED_CAST") // we know the two types are the same and comparable
      val order = (value1 as Comparable<Any>).compareTo(value2)
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
