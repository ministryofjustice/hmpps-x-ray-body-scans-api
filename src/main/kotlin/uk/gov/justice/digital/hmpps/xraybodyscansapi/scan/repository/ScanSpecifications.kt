package uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.repository

import org.springframework.data.jpa.domain.Specification
import java.time.LocalDate
import kotlin.reflect.KProperty1

/** Build «equal to» specification from an entity’s property */
private fun <T : Any, V : Any> KProperty1<T, V?>.buildSpecForEqualTo(value: V): Specification<T> = Specification { root, _, criteriaBuilder ->
  criteriaBuilder.equal(root.get<V>(name), value)
}

/** Build «less than or equal to» specification from an entity’s property */
private fun <T : Any, V : Comparable<V>> KProperty1<T, V>.buildSpecForLessThan(value: V): Specification<T> = Specification { root, _, criteriaBuilder ->
  criteriaBuilder.lessThanOrEqualTo(root.get(name), value)
}

/** Build «greater than or equal to» specification from an entity’s property */
private fun <T : Any, V : Comparable<V>> KProperty1<T, V>.buildSpecForGreaterThanOrEqualTo(value: V): Specification<T> = Specification { root, _, criteriaBuilder ->
  criteriaBuilder.greaterThanOrEqualTo(root.get(name), value)
}

fun filterByPrisonerNumber(prisonerNumber: String) = ScanEntity::prisonerNumber.buildSpecForEqualTo(prisonerNumber)
fun filterFromScanDate(fromScanDate: LocalDate) = ScanEntity::scanDate.buildSpecForGreaterThanOrEqualTo(fromScanDate)
fun filterToScanDate(toScanDate: LocalDate) = ScanEntity::scanDate.buildSpecForLessThan(toScanDate)
