package uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.service

sealed interface IncludeAlerts {
  object No : IncludeAlerts
  data class WithUsername(val username: String) : IncludeAlerts

  companion object {
    fun from(includeAlerts: Boolean, getUsername: () -> String): IncludeAlerts = if (includeAlerts) {
      WithUsername(getUsername())
    } else {
      No
    }
  }
}
