package uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.repository

interface ScanSummaryRow {
  val prisonerNumber: String
  val outcome: String
  val count: Int
}

fun Collection<ScanSummaryRow>.groupOutcomes(): Map<String, Map<String, Int>> = groupBy({ it.prisonerNumber })
  .mapValues { rows ->
    rows.value.associate { it.outcome to it.count }
  }
