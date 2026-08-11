package uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.scan.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.service.IncludeAlerts

@DisplayName("Request to include alerts in scan summaries")
class IncludeAlertsTest {
  @Test
  fun `represents 'no' when not requested`() {
    val includeAlerts = IncludeAlerts.from(false) {
      fail("username getter should not be called")
    }
    assertThat(includeAlerts).isEqualTo(IncludeAlerts.No)
  }

  @Test
  fun `retrieves username when requested`() {
    val includeAlerts = IncludeAlerts.from(true) { "user3" }
    assertThat(includeAlerts).isEqualTo(IncludeAlerts.WithUsername("user3"))
  }
}
