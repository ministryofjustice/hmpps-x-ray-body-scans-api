package uk.gov.justice.digital.hmpps.xraybodyscansapi.config

import org.springframework.security.access.prepost.PreAuthorize

/** Read-only access to x-ray body scans */
const val ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RO = "ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RO"

/** Read-write access to x-ray body scans */
const val ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RW = "ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RW"

@PreAuthorize("hasAnyRole('$ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RO', '$ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RW')")
annotation class RequireReadRole

@PreAuthorize("hasRole('$ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RW')")
annotation class RequireWriteRole
