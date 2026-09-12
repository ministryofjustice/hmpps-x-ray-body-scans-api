package uk.gov.justice.digital.hmpps.xraybodyscansapi.jpa

import org.springframework.data.jpa.domain.Specification
import kotlin.reflect.KProperty1

/** Build «equal to» specification from an entity’s property */
fun <T : Any, V : Any> KProperty1<T, V?>.buildSpecForEqualTo(value: V): Specification<T> = Specification { root, _, criteriaBuilder ->
  criteriaBuilder.equal(root.get<V>(name), value)
}

/** Build «is null» specification from an entity’s property */
fun <T : Any, V : Any> KProperty1<T, V?>.buildSpecForIsNull(): Specification<T> = Specification { root, _, _ ->
  root.get<V>(name).isNull
}

/** Build «in» specification from an entity’s property */
fun <T : Any, V : Any> KProperty1<T, V?>.buildSpecForIn(values: Collection<V>): Specification<T> = Specification { root, _, _ ->
  root.get<V>(name).`in`(values)
}

/** Build «less than or equal to» specification from an entity’s property */
fun <T : Any, V : Comparable<V>> KProperty1<T, V>.buildSpecForLessThan(value: V): Specification<T> = Specification { root, _, criteriaBuilder ->
  criteriaBuilder.lessThanOrEqualTo(root.get(name), value)
}

/** Build «greater than or equal to» specification from an entity’s property */
fun <T : Any, V : Comparable<V>> KProperty1<T, V>.buildSpecForGreaterThanOrEqualTo(value: V): Specification<T> = Specification { root, _, criteriaBuilder ->
  criteriaBuilder.greaterThanOrEqualTo(root.get(name), value)
}
