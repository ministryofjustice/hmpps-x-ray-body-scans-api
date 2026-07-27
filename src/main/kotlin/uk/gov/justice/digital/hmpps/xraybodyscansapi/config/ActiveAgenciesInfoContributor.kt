package uk.gov.justice.digital.hmpps.xraybodyscansapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.info.Info
import org.springframework.boot.actuate.info.InfoContributor
import org.springframework.stereotype.Component

@Component
class ActiveAgenciesInfoContributor(
  // TODO: currently saved in yaml configuration but will move to database to store dates when each prison goes live
  @param:Value($$"${service.active-agencies:}")
  val activeAgencies: List<String>,
) : InfoContributor {
  override fun contribute(builder: Info.Builder) {
    builder.withDetail("activeAgencies", activeAgencies)
  }
}
