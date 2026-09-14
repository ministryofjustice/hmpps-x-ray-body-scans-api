package uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

abstract class MockServerExtension(private val mockServer: WireMockServer) :
  BeforeAllCallback,
  BeforeEachCallback,
  AfterAllCallback {
  override fun beforeAll(context: ExtensionContext) {
    mockServer.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    mockServer.resetRequests()
  }

  override fun afterAll(context: ExtensionContext) {
    mockServer.stop()
  }
}
