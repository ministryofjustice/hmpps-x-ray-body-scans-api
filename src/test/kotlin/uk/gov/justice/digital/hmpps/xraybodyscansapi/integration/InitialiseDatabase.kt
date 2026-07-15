package uk.gov.justice.digital.hmpps.xraybodyscansapi.integration

import org.junit.jupiter.api.Test

class InitialiseDatabase : IntegrationTestBase() {
  @Test
  fun `initialises database`() {
    println("Database has been initialised by IntegrationTestBase")
  }
}
