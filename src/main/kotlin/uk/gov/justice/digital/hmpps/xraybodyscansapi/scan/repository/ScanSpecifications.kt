package uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.repository

import uk.gov.justice.digital.hmpps.xraybodyscansapi.jpa.buildSpecForEqualTo
import uk.gov.justice.digital.hmpps.xraybodyscansapi.jpa.buildSpecForGreaterThanOrEqualTo
import uk.gov.justice.digital.hmpps.xraybodyscansapi.jpa.buildSpecForLessThan
import java.time.LocalDate

fun filterByPrisonerNumber(prisonerNumber: String) = ScanEntity::prisonerNumber.buildSpecForEqualTo(prisonerNumber)

fun filterFromScanDate(fromScanDate: LocalDate) = ScanEntity::scanDate.buildSpecForGreaterThanOrEqualTo(fromScanDate)

fun filterToScanDate(toScanDate: LocalDate) = ScanEntity::scanDate.buildSpecForLessThan(toScanDate)
