package uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.util

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.data.domain.Sort
import uk.gov.justice.digital.hmpps.xraybodyscansapi.util.SortComparator
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicLong
import kotlin.reflect.KProperty1

class SortComparatorTest {
  private val today: LocalDate = LocalDate.now()

  private class ExampleComparator(sort: Sort) : SortComparator<Example>(sort) {
    override fun mapToProperty(name: String): KProperty1<Example, Comparable<*>?> = when (name) {
      "id" -> Example::id
      "date" -> Example::date
      else -> throw IllegalArgumentException("cannot sort examples by $name")
    }
  }

  @Test
  fun `sorts empty list`() {
    val examples = emptyList<Example>()
    val sorted = examples.sortedWith(
      ExampleComparator(
        Sort.by("id").descending(),
      ),
    )
    assertThat(sorted).isEmpty()
  }

  @Test
  fun `throws when sorting by unknown property`() {
    assertThatThrownBy {
      ExampleComparator(
        Sort.by("name").descending(),
      )
    }.hasMessage("cannot sort examples by name")
  }

  @Test
  fun `sorts objects by descending date`() {
    val examples = mutableListOf(
      Example(today.minusDays(1)),
      Example(today),
      Example(today.minusDays(10)),
      Example(today.minusDays(2)),
    )
    examples.sortWith(ExampleComparator(Sort.by("date").descending()))
    assertThat(examples.map { it.date }).containsExactly(
      today,
      today.minusDays(1),
      today.minusDays(2),
      today.minusDays(10),
    )
  }

  @Test
  fun `sorts objects by ascending date`() {
    val examples = mutableListOf(
      Example(today.minusDays(1)),
      Example(today),
      Example(today.minusDays(10)),
      Example(today.minusDays(2)),
    )
    examples.sortWith(ExampleComparator(Sort.by("date").ascending()))
    assertThat(examples.map { it.date }).containsExactly(
      today.minusDays(10),
      today.minusDays(2),
      today.minusDays(1),
      today,
    )
  }

  @ParameterizedTest(name = "sorts objects {0} with nulls last")
  @ValueSource(strings = ["by descending date", "by ascending date"])
  fun `sorts objects with nulls last`(scenario: String) {
    val examples = mutableListOf(
      Example(null),
      Example(today.minusDays(1)),
      Example(today),
      Example(null),
      Example(today.minusDays(2)),
    )
    val comparator = ExampleComparator(
      if (scenario == "by descending date") {
        Sort.by("date").ascending()
      } else {
        Sort.by("date").descending()
      },
    )
    examples.sortWith(comparator)
    assertThat(examples.map { it.date }.takeLast(2)).containsExactly(
      null,
      null,
    )
  }

  @Test
  fun `sorts objects by descending date and id`() {
    val examples = mutableListOf(
      Example(id = 1, date = today),
      Example(id = 2, date = today),
      Example(id = 3, date = null),
      Example(id = 4, date = today.minusDays(2)),
      Example(id = 5, date = today.minusDays(2)),
    )
    examples.sortWith(ExampleComparator(Sort.by("date", "id").descending()))
    assertThat(examples.map { it.id to it.date }).containsExactly(
      2L to today,
      1L to today,
      5L to today.minusDays(2),
      4L to today.minusDays(2),
      3L to null,
    )
  }

  @Test
  fun `sorts objects by ascending date and id`() {
    val examples = mutableListOf(
      Example(id = 1, date = today),
      Example(id = 2, date = today),
      Example(id = 3, date = null),
      Example(id = 4, date = today.minusDays(2)),
      Example(id = 5, date = today.minusDays(2)),
    )
    examples.sortWith(ExampleComparator(Sort.by("date", "id").ascending()))
    assertThat(examples.map { it.id to it.date }).containsExactly(
      4L to today.minusDays(2),
      5L to today.minusDays(2),
      1L to today,
      2L to today,
      3L to null,
    )
  }

  private data class Example(
    val id: Long,
    val date: LocalDate?,
  ) {
    companion object {
      private val idGenerator = AtomicLong()
    }

    constructor(date: LocalDate?) : this(idGenerator.incrementAndGet(), date)
  }
}
