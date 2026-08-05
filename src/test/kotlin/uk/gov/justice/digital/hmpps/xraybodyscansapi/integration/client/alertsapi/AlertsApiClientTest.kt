package uk.gov.justice.digital.hmpps.xraybodyscansapi.client.alertsapi

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.xraybodyscansapi.client.alertsapi.response.Alert
import uk.gov.justice.digital.hmpps.xraybodyscansapi.client.alertsapi.response.AlertCode
import uk.gov.justice.digital.hmpps.xraybodyscansapi.client.alertsapi.response.AlertResponse
import uk.gov.justice.digital.hmpps.xraybodyscansapi.config.DownstreamServiceException
import uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.wiremock.AlertsApiExtension
import uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.wiremock.AlertsApiExtension.Companion.alertsApi

@ExtendWith(AlertsApiExtension::class)
class AlertsApiClientTest {
  private lateinit var client: AlertsApiClient

  @BeforeEach
  fun resetMocks() {
    val webClient = WebClient.create("http://localhost:${alertsApi.port()}")
    client = AlertsApiClient(webClient)
  }

  val prisonerNumbers = listOf("A1234AA", "B1234BB")

  @Test
  fun `getAlerts returns matching alerts`() {
    alertsApi.stubBulkGetAlerts(
      // language=json
      """
      {"content":  [
        {"alertUuid":  "019fcc21-8aaf-75a8-9c27-ec1e006fe35e", "prisonNumber": "A1234AA", "alertCode":  {
          "alertTypeCode": "X", "alertTypeDescription": "Security", "code": "XIS", "description": "Internal Secretor"
        }, "description":  ""},
        {"alertUuid":  "019fcc21-8df6-7278-8869-8fe992a46c68", "prisonNumber": "B1234BB", "alertCode":  {
          "alertTypeCode": "X", "alertTypeDescription": "Security", "code": "XXRAY", "description": "Do Not X-Ray Body Scan"
        }, "description":  ""}
      ]}
      """,
    )

    val result = client.getAlerts(prisonerNumbers)

    assertThat(result).isEqualTo(
      AlertResponse(
        listOf(
          Alert(
            alertUuid = "019fcc21-8aaf-75a8-9c27-ec1e006fe35e",
            prisonNumber = "A1234AA",
            alertCode = AlertCode(
              alertTypeCode = "X",
              alertTypeDescription = "Security",
              code = "XIS",
              description = "Internal Secretor",
            ),
            description = "",
          ),
          Alert(
            alertUuid = "019fcc21-8df6-7278-8869-8fe992a46c68",
            prisonNumber = "B1234BB",
            alertCode = AlertCode(
              alertTypeCode = "X",
              alertTypeDescription = "Security",
              code = "XXRAY",
              description = "Do Not X-Ray Body Scan",
            ),
            description = "",
          ),
        ),
      ),
    )
  }

  @Test
  fun `getAlerts returns empty list`() {
    alertsApi.stubBulkGetAlerts(
      // language=json
      """
      {"content":  []}
      """,
    )

    val result = client.getAlerts(prisonerNumbers)

    assertThat(result).isEqualTo(AlertResponse(emptyList()))
  }

  @Test
  fun `getAlerts returns empty list when api returns empty body`() {
    alertsApi.stubBulkGetAlerts("")

    val result = client.getAlerts(prisonerNumbers)

    assertThat(result).isEqualTo(AlertResponse(emptyList()))
  }

  @ParameterizedTest(name = "getAlerts throws on {0}")
  @ValueSource(ints = [404, 500])
  fun `getAlerts throws on non-200 status`(status: Int) {
    alertsApi.stubBulkGetAlertsError(status)

    assertThatThrownBy { client.getAlerts(prisonerNumbers) }
      .isInstanceOf(DownstreamServiceException::class.java)
      .hasMessage("Alerts API bulk-get alerts request failed")
      .cause()
      .extracting { (it as WebClientResponseException).statusCode.value() }
      .isEqualTo(status)
  }
}
