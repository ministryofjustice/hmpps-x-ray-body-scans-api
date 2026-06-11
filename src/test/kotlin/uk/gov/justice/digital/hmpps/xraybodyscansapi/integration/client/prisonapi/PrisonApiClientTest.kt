package uk.gov.justice.digital.hmpps.xraybodyscansapi.client.prisonapi

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.xraybodyscansapi.client.prisonapi.response.PersonalCareNeed
import uk.gov.justice.digital.hmpps.xraybodyscansapi.client.prisonapi.response.PersonalCareNeedsResponse
import uk.gov.justice.digital.hmpps.xraybodyscansapi.config.DownstreamServiceException
import uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.wiremock.PrisonApiMockServer
import java.time.LocalDate

class PrisonApiClientTest {
  private lateinit var client: PrisonApiClient

  private val offenderNos = listOf("A1234AA", "B1234BB")

  @BeforeEach
  fun resetMocks() {
    server.resetRequests()
    val webClient = WebClient.create("http://localhost:${server.port()}")
    client = PrisonApiClient(webClient)
  }

  @Test
  fun `getScanCareNeeds returns personal care needs for each offender`() {
    server.stubGetScanCareNeeds(
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
    server.stubGetScanCareNeedsError(404)

    assertThatThrownBy { client.getScanCareNeeds(offenderNos) }
      .isInstanceOf(DownstreamServiceException::class.java)
      .hasMessage("Prison API get scans request failed")
      .cause()
      .extracting { (it as WebClientResponseException).statusCode.value() }
      .isEqualTo(404)
  }

  @Test
  fun `getScanCareNeeds throws on 500`() {
    server.stubGetScanCareNeedsError(500)

    assertThatThrownBy { client.getScanCareNeeds(offenderNos) }
      .isInstanceOf(DownstreamServiceException::class.java)
      .hasMessage("Prison API get scans request failed")
      .cause()
      .extracting { (it as WebClientResponseException).statusCode.value() }
      .isEqualTo(500)
  }

  companion object {
    @JvmField
    internal val server = PrisonApiMockServer()

    @BeforeAll
    @JvmStatic
    fun startMocks() {
      server.start()
    }

    @AfterAll
    @JvmStatic
    fun stopMocks() {
      server.stop()
    }
  }
}
