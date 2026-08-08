package uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.util

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.response.UnifiedScanResponse
import kotlin.sequences.sequence as buildSequence

/**
 * Merges two sequences of scans using a Spring Data sort directive
 * and lazily evaluates them to get the desired page
 */
class UnifiedScanResponsePaginator<T : UnifiedScanResponse>(
  /** The number of scans in the sequence if it were read to completion. */
  val totalCount: Int,
  /** Sequence of scans to be merged. NB: must be pre-sorted! */
  val sequence: Sequence<T>,
) : Sequence<T> {
  override fun iterator(): Iterator<T> = sequence.iterator()

  fun <U : UnifiedScanResponse> paginateWith(
    other: UnifiedScanResponsePaginator<U>,
    pageable: Pageable,
  ): Page<out UnifiedScanResponse> {
    val combinedTotalCount = totalCount + other.totalCount

    val iterator1 = iterator()
    val iterator2 = other.iterator()

    fun getNext1() = if (iterator1.hasNext()) iterator1.next() else null
    fun getNext2() = if (iterator2.hasNext()) iterator2.next() else null

    val comparator = UnifiedScanResponseComparator(pageable.sort)
    val mergedSequence = buildSequence {
      var next1 = getNext1()
      var next2 = getNext2()
      while (next1 != null && next2 != null) {
        val comparison = comparator.compare(next1, next2)
        if (comparison <= 0) {
          yield(next1)
          next1 = getNext1()
        } else {
          yield(next2)
          next2 = getNext2()
        }
      }
      next1?.let {
        yield(next1)
        yieldAll(iterator1)
      }
      next2?.let {
        yield(next2)
        yieldAll(iterator2)
      }
    }

    val scans = mergedSequence.chunked(pageable.pageSize)
      .elementAtOrNull(pageable.pageNumber)
      ?: emptyList()

    return PageImpl(scans, pageable, combinedTotalCount.toLong())
  }
}
