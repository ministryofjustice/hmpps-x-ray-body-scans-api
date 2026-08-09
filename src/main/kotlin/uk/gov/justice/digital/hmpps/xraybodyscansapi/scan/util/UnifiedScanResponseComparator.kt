package uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.util

import org.springframework.data.domain.Sort
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.response.UnifiedScanResponse
import uk.gov.justice.digital.hmpps.xraybodyscansapi.util.SortComparator

class UnifiedScanResponseComparator(sort: Sort) : SortComparator<UnifiedScanResponse>(sort) {
  override fun getterForProperty(name: String): (UnifiedScanResponse) -> Comparable<*>? = when (name) {
    "id" -> UnifiedScanResponse::id
    "scanDate" -> UnifiedScanResponse::scanDate
    "source" -> UnifiedScanResponse::source
    else -> throw IllegalArgumentException("cannot sort scans by $name")
  }
}
