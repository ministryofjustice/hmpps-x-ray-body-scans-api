package uk.gov.justice.digital.hmpps.xraybodyscansapi.client.prisonapi

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.xraybodyscansapi.client.prisonapi.response.PersonalCareNeed
import uk.gov.justice.digital.hmpps.xraybodyscansapi.client.prisonapi.response.PersonalCareNeedsResponse
import uk.gov.justice.digital.hmpps.xraybodyscansapi.config.DownstreamServiceException
import uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.wiremock.PrisonApiExtension
import uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.wiremock.PrisonApiExtension.Companion.prisonApi
import java.time.LocalDate

@ExtendWith(PrisonApiExtension::class)
class PrisonApiClientTest {
  private lateinit var client: PrisonApiClient

  private val offenderNos = listOf("A1234AA", "B1234BB")

  @BeforeEach
  fun resetMocks() {
    val webClient = WebClient.create("http://localhost:${prisonApi.port()}")
    client = PrisonApiClient(webClient)
  }

  @Test
  fun `getScanCareNeeds returns personal care needs for each offender`() {
    prisonApi.stubGetScanCareNeeds(
      // language=json
      """
      [
        {
          "offenderNo": "A1234AA",
          "personalCareNeeds": [
            { "personalCareNeedId": 1, "problemType": "BSCAN", "problemCode": "xyz", "problemStatus": "xyz", "startDate": "2026-01-01" },
            { "personalCareNeedId": 2, "problemType": "BSCAN", "problemCode": "xyz", "problemStatus": "xyz", "startDate": "2026-06-09" }
          ]
        },
        {
          "offenderNo": "B1234BB",
          "personalCareNeeds": [
            { "personalCareNeedId": 3, "problemType": "BSCAN", "problemCode": "xyz", "problemStatus": "xyz", "startDate": "2026-12-31" }
          ]
        }
      ]
      """,
    )

    val result = client.getScanCareNeeds(offenderNos)

    assertThat(result).containsExactly(
      PersonalCareNeedsResponse(
        offenderNo = "A1234AA",
        personalCareNeeds = listOf(
          PersonalCareNeed(personalCareNeedId = 1, problemType = "BSCAN", problemCode = "xyz", problemStatus = "xyz", startDate = LocalDate.parse("2026-01-01")),
          PersonalCareNeed(personalCareNeedId = 2, problemType = "BSCAN", problemCode = "xyz", problemStatus = "xyz", startDate = LocalDate.parse("2026-06-09")),
        ),
      ),
      PersonalCareNeedsResponse(
        offenderNo = "B1234BB",
        personalCareNeeds = listOf(
          PersonalCareNeed(personalCareNeedId = 3, problemType = "BSCAN", problemCode = "xyz", problemStatus = "xyz", startDate = LocalDate.parse("2026-12-31")),
        ),
      ),
    )
  }

  @Test
  fun `getScanCareNeeds throws on 404`() {
    prisonApi.stubGetScanCareNeedsError(404)

    assertThatThrownBy { client.getScanCareNeeds(offenderNos) }
      .isInstanceOf(DownstreamServiceException::class.java)
      .hasMessage("Prison API get scans request failed")
      .cause()
      .extracting { (it as WebClientResponseException).statusCode.value() }
      .isEqualTo(404)
  }

  @Test
  fun `getScanCareNeeds throws on 500`() {
    prisonApi.stubGetScanCareNeedsError(500)

    assertThatThrownBy { client.getScanCareNeeds(offenderNos) }
      .isInstanceOf(DownstreamServiceException::class.java)
      .hasMessage("Prison API get scans request failed")
      .cause()
      .extracting { (it as WebClientResponseException).statusCode.value() }
      .isEqualTo(500)
  }
}
