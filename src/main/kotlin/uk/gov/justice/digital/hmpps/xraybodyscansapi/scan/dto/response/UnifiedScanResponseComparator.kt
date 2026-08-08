package uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.response

import org.springframework.data.domain.Sort
import uk.gov.justice.digital.hmpps.xraybodyscansapi.util.SortComparator
import kotlin.reflect.KProperty1

class UnifiedScanResponseComparator(sort: Sort) : SortComparator<UnifiedScanResponse>(sort) {
  override fun mapToProperty(name: String): KProperty1<UnifiedScanResponse, Comparable<*>?> = when (name) {
    "id" -> UnifiedScanResponse::id
    "scanDate" -> UnifiedScanResponse::scanDate
    else -> throw IllegalArgumentException("cannot sort scans by $name")
  }
}
