package uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.sar

import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Test
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
import javax.sql.DataSource

@Import(SarIntegrationTestHelperConfig::class)
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
  override fun setupTestData() {}

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
