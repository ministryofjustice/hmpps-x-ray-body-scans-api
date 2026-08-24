package uk.gov.justice.digital.hmpps.xraybodyscansapi.config

import org.springframework.security.access.prepost.PreAuthorize

/** Read-only access to x-ray body scans */
const val ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RO = "ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RO"

/** Read-write access to x-ray body scans */
const val ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RW = "ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RW"

/** Read-write access to case notes associated with x-ray body scans */
const val ROLE_X_RAY_BODY_SCANS_API__SCAN_CASE_NOTE__RW = "ROLE_X_RAY_BODY_SCANS_API__CASE_NOTE_DATA__RW"

/** Read-only access to case notes associated with x-ray body scans */
const val ROLE_X_RAY_BODY_SCANS_API__SCAN_CASE_NOTE__RO = "ROLE_X_RAY_BODY_SCANS_API__CASE_NOTE_DATA__RO"

@PreAuthorize("hasAnyRole('$ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RO', '$ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RW')")
annotation class RequireReadRole

@PreAuthorize("hasRole('$ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RW')")
annotation class RequireWriteRole

@PreAuthorize("hasAnyRole('$ROLE_X_RAY_BODY_SCANS_API__SCAN_CASE_NOTE__RO', '$ROLE_X_RAY_BODY_SCANS_API__SCAN_CASE_NOTE__RW')")
annotation class RequireReadScanNoteRole

@PreAuthorize("hasRole('$ROLE_X_RAY_BODY_SCANS_API__SCAN_CASE_NOTE__RW')")
annotation class RequireWriteScanNoteRole
