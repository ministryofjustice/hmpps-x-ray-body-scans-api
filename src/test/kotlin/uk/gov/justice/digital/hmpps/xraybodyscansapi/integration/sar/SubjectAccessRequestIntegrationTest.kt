package uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.sar

import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarApiDataTest
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarFlywaySchemaTest
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarIntegrationTestHelper
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarIntegrationTestHelperConfig
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarJpaEntitiesTest
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarReportTest
import uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.wiremock.CaseNotesApiExtension
import uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.wiremock.CaseNotesApiExtension.Companion.caseNotesApi
import uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.wiremock.HmppsAuthApiExtension
import uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.wiremock.HmppsAuthApiExtension.Companion.hmppsAuth
import javax.sql.DataSource

@Import(SarIntegrationTestHelperConfig::class)
@ExtendWith(CaseNotesApiExtension::class, HmppsAuthApiExtension::class)
class SubjectAccessRequestIntegrationTest :
  IntegrationTestBase(),
  SarFlywaySchemaTest,
  SarJpaEntitiesTest,
  SarApiDataTest,
  SarReportTest {

  @Autowired
  lateinit var dataSource: DataSource

  @Autowired
  lateinit var entityManager: EntityManager

  @Autowired
  lateinit var sarIntegrationTestHelper: SarIntegrationTestHelper

  override fun getSarHelper(): SarIntegrationTestHelper = sarIntegrationTestHelper
  override fun getWebTestClientInstance(): WebTestClient = webTestClient
  override fun getDataSourceInstance(): DataSource = dataSource
  override fun getEntityManagerInstance(): EntityManager = entityManager
  override fun getPrn(): String = "A1234BC"
  override fun setupTestData() {
    hmppsAuth.stubGrantToken()
    caseNotesApi.stubGetCaseNote(
      """
        {
          "caseNoteId": "01a067dc-332f-754e-b41f-d8fe1eaeba89",
          "offenderIdentifier": "${getPrn()}",
          "type": "GEN",
          "typeDescription": "General",
          "subType": "XRBS",
          "subTypeDescription": "X-Ray Body Scan",
          "text": "The outcome was Item detected and further actions were taken.",
          "creationDateTime": "2026-01-01T00:00:00",
          "occurrenceDateTime": "2026-01-01T00:00:00",
          "authorName": "Author",
          "amendments": [
            { "additionalNoteText": "Follow up actions have been taken against this person" }
          ]
        }
      """.trimIndent(),
      getPrn(),
      "01a067dc-332f-754e-b41f-d8fe1eaeba89"
    )
  }

  @Test
  @Sql("classpath:sar/test-data/reset.sql")
  @Sql("classpath:sar/test-data/scans.sql")
  override fun `SAR API should return expected data`() {
    super.`SAR API should return expected data`()
  }

  @Test
  @Sql("classpath:sar/test-data/reset.sql")
  @Sql("classpath:sar/test-data/scans.sql")
  override fun `SAR report should render as expected`() {
    super.`SAR report should render as expected`()
  }
}
