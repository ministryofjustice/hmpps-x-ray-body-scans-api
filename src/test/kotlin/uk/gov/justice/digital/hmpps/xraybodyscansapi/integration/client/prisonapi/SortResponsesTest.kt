package uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.client.prisonapi

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.data.domain.Sort
import uk.gov.justice.digital.hmpps.xraybodyscansapi.client.prisonapi.response.PersonalCareNeed
import uk.gov.justice.digital.hmpps.xraybodyscansapi.client.prisonapi.response.sortWith
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicLong

class SortResponsesTest {
  private val idGenerator = AtomicLong()
  private val today: LocalDate = LocalDate.now()

  @Test
  fun `sorts empty list`() {
    val careNeeds = mutableListOf<PersonalCareNeed>()
    careNeeds.sortWith(Sort.by("scanDate").descending())
    assertThat(careNeeds).isEmpty()
  }

  @Test
  fun `sorts care needs descending by date`() {
    val careNeeds = mutableListOf(
      careNeed(startDate = today.minusDays(1)),
      careNeed(startDate = today),
      careNeed(startDate = today.minusDays(10)),
      careNeed(startDate = today.minusDays(2)),
    )
    careNeeds.sortWith(Sort.by("scanDate").descending())
    assertThat(careNeeds.map { it.startDate }).containsExactly(
      today,
      today.minusDays(1),
      today.minusDays(2),
      today.minusDays(10),
    )
  }

  @Test
  fun `sorts care needs ascending by date`() {
    val careNeeds = mutableListOf(
      careNeed(startDate = today.minusDays(1)),
      careNeed(startDate = today),
      careNeed(startDate = today.minusDays(10)),
      careNeed(startDate = today.minusDays(2)),
    )
    careNeeds.sortWith(Sort.by("scanDate").ascending())
    assertThat(careNeeds.map { it.startDate }).containsExactly(
      today.minusDays(10),
      today.minusDays(2),
      today.minusDays(1),
      today,
    )
  }

  @ParameterizedTest(name = "sorts care needs with null dates last, ascending by date is {0}")
  @ValueSource(booleans = [true, false])
  fun `sorts care needs with null dates last`(ascending: Boolean) {
    val careNeeds = mutableListOf(
      careNeed(startDate = null),
      careNeed(startDate = today.minusDays(1)),
      careNeed(startDate = today),
      careNeed(startDate = null),
      careNeed(startDate = today.minusDays(2)),
    )
    careNeeds.sortWith(
      if (ascending) {
        Sort.by("scanDate").ascending()
      } else {
        Sort.by("scanDate").descending()
      },
    )
    assertThat(careNeeds.map { it.startDate }.takeLast(2)).containsExactly(
      null,
      null,
    )
  }

  @Test
  fun `sorts care needs by descending date and id`() {
    val careNeeds = mutableListOf(
      careNeed(id = 1, startDate = today),
      careNeed(id = 2, startDate = today),
      careNeed(id = 3, startDate = null),
      careNeed(id = 4, startDate = today.minusDays(2)),
      careNeed(id = 5, startDate = today.minusDays(2)),
    )
    careNeeds.sortWith(Sort.by("scanDate", "id").descending())
    assertThat(careNeeds.map { it.personalCareNeedId to it.startDate }).containsExactly(
      2L to today,
      1L to today,
      5L to today.minusDays(2),
      4L to today.minusDays(2),
      3L to null,
    )
  }

  @Test
  fun `sorts care needs by ascending date and id`() {
    val careNeeds = mutableListOf(
      careNeed(id = 1, startDate = today),
      careNeed(id = 2, startDate = today),
      careNeed(id = 3, startDate = null),
      careNeed(id = 4, startDate = today.minusDays(2)),
      careNeed(id = 5, startDate = today.minusDays(2)),
    )
    careNeeds.sortWith(Sort.by("scanDate", "id").ascending())
    assertThat(careNeeds.map { it.personalCareNeedId to it.startDate }).containsExactly(
      4L to today.minusDays(2),
      5L to today.minusDays(2),
      1L to today,
      2L to today,
      3L to null,
    )
  }

  private fun careNeed(startDate: LocalDate?, id: Long? = null) = PersonalCareNeed(
    personalCareNeedId = id ?: idGenerator.incrementAndGet(),
    problemType = "BSCAN",
    problemCode = "BSC6.0",
    problemStatus = "ON",
    problemDescription = "Body Scan (6.0 µSv)",
    commentText = "notes",
    startDate = startDate,
    endDate = null,
  )
}
