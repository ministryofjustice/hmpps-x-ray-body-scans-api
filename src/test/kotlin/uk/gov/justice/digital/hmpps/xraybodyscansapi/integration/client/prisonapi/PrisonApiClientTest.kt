package uk.gov.justice.digital.hmpps.xraybodyscansapi.client.prisonapi

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.xraybodyscansapi.client.prisonapi.response.CountNomisScansResponse
import uk.gov.justice.digital.hmpps.xraybodyscansapi.config.DownstreamServiceException
import uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.wiremock.PrisonApiMockServer
import java.time.LocalDate

class PrisonApiClientTest {
  private lateinit var client: PrisonApiClient

  private val offenderNos = listOf("A1234AA", "B1234BB")
  private val fromStartDate = LocalDate.parse("2024-01-01")
  private val toStartDate = LocalDate.parse("2024-12-31")

  @BeforeEach
  fun resetMocks() {
    server.resetRequests()
    val webClient = WebClient.create("http://localhost:${server.port()}")
    client = PrisonApiClient(webClient)
  }

  @Test
  fun `countNomisScans gets scan counts if the data is available`() {
    server.stubCountNomisScans(
      """
      [
        { "offenderNo": "A1234AA", "size": 2 },
        { "offenderNo": "B1234BB", "size": 5 }
      ]
      """,
    )

    val result = client.countNomisScans(offenderNos, fromStartDate, toStartDate)

    assertThat(result).containsExactly(
      CountNomisScansResponse(offenderNo = "A1234AA", size = 2),
      CountNomisScansResponse(offenderNo = "B1234BB", size = 5),
    )
  }

  @Test
  fun `countNomisScans throws on 404 (considered an error because api should return an empty array instead)`() {
    server.stubCountNomisScansError(404)

    assertThatThrownBy { client.countNomisScans(offenderNos, fromStartDate, toStartDate) }
      .isInstanceOf(DownstreamServiceException::class.java)
      .hasMessage("Prison API count scans request failed")
      .cause()
      .extracting { (it as WebClientResponseException).statusCode.value() }
      .isEqualTo(404)
  }

  @Test
  fun `countNomisScans throws on 500`() {
    server.stubCountNomisScansError(500)

    assertThatThrownBy { client.countNomisScans(offenderNos, fromStartDate, toStartDate) }
      .isInstanceOf(DownstreamServiceException::class.java)
      .hasMessage("Prison API count scans request failed")
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
