package uk.gov.justice.digital.hmpps.xraybodyscansapi.config

import org.springframework.security.access.prepost.PreAuthorize

/** Read-only access to x-ray body scans */
const val READ_ROLE = "ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RO"

/** Read-write access to x-ray body scans */
const val WRITE_ROLE = "ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RW"

/** Read-only access to case notes associated with x-ray body scans */
const val READ_CASE_NOTE_ROLE = "ROLE_X_RAY_BODY_SCANS_API__CASE_NOTE_DATA__RO"

/** Read-write access to case notes associated with x-ray body scans */
const val WRITE_CASE_NOTE_ROLE = "ROLE_X_RAY_BODY_SCANS_API__CASE_NOTE_DATA__RW"

/** Admin access to x-ray body scans – should not be granted to most services */
const val ADMIN_ROLE = "ROLE_X_RAY_BODY_SCANS_API__ADMIN"

@PreAuthorize("hasAnyRole('$READ_ROLE', '$WRITE_ROLE', '$ADMIN_ROLE')")
annotation class RequireReadRole

@PreAuthorize("hasAnyRole('$WRITE_ROLE', '$ADMIN_ROLE')")
annotation class RequireWriteRole

@PreAuthorize("hasAnyRole('$READ_CASE_NOTE_ROLE', '$WRITE_CASE_NOTE_ROLE', '$ADMIN_ROLE')")
annotation class RequireReadScanNoteRole

@PreAuthorize("hasAnyRole('$WRITE_CASE_NOTE_ROLE', '$ADMIN_ROLE')")
annotation class RequireWriteScanNoteRole

@PreAuthorize("hasRole('$ADMIN_ROLE')")
annotation class RequireAdminRole
