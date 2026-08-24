package uk.gov.justice.digital.hmpps.xraybodyscansapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.endpoint.RestClientClientCredentialsTokenResponseClient
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.web.context.annotation.RequestScope
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClient.Builder
import uk.gov.justice.hmpps.kotlin.auth.authorisedWebClient
import uk.gov.justice.hmpps.kotlin.auth.healthWebClient
import java.time.Duration

@Configuration
class WebClientConfiguration(
  @Value($$"${api.hmpps-auth.base-url}") private val hmppsAuthBaseUri: String,
  @Value($$"${api.hmpps-auth.health-timeout:20s}") private val hmppsAuthHealthTimeout: Duration,

  @Value($$"${api.prison-api.base-url}") private val prisonApiBaseUri: String,
  @Value($$"${api.prison-api.timeout:30s}") private val prisonApiTimeout: Duration,
  @Value($$"${api.prison-api.health-timeout:20s}") private val prisonApiHealthTimeout: Duration,

  @Value($$"${api.alerts-api.base-url}") private val alertsApiBaseUri: String,
  @Value($$"${api.alerts-api.timeout:30s}") private val alertsApiTimeout: Duration,
  @Value($$"${api.alerts-api.health-timeout:20s}") private val alertsApiHealthTimeout: Duration,

  @Value($$"${api.case-notes-api.base-url}") private val caseNotesApiBaseUri: String,
  @Value($$"${api.case-notes-api.timeout:30s}") private val caseNotesApiTimeout: Duration,
  @Value($$"${api.case-notes-api.health-timeout:20s}") private val caseNotesApiHealthTimeout: Duration,
) {
  @Bean
  fun hmppsAuthHealthWebClient(builder: Builder): WebClient = builder.healthWebClient(hmppsAuthBaseUri, hmppsAuthHealthTimeout)

  @Bean
  fun caseNotesApiHealthWebClient(builder: Builder): WebClient = builder.healthWebClient(caseNotesApiBaseUri, caseNotesApiHealthTimeout)

  @Bean
  fun prisonApiHealthWebClient(builder: Builder): WebClient = builder.healthWebClient(prisonApiBaseUri, prisonApiHealthTimeout)

  @Bean
  fun alertsApiHealthWebClient(builder: Builder): WebClient = builder.healthWebClient(alertsApiBaseUri, alertsApiHealthTimeout)

  @Bean
  @RequestScope
  fun prisonApiWebClient(
    clientRegistrationRepository: ClientRegistrationRepository,
    builder: Builder,
  ) = builder.authorisedWebClient(
    authorizedClientManagerUserEnhanced(clientRegistrationRepository),
    "hmpps-x-ray-body-scans-api",
    prisonApiBaseUri,
    prisonApiTimeout,
  )

  @Bean
  @RequestScope
  fun alertsApiWebClient(
    clientRegistrationRepository: ClientRegistrationRepository,
    builder: Builder,
  ) = builder.authorisedWebClient(
    authorizedClientManagerUserEnhanced(clientRegistrationRepository),
    "hmpps-x-ray-body-scans-api",
    alertsApiBaseUri,
    alertsApiTimeout,
  )

  @Bean
  @RequestScope
  fun caseNotesApiWebClient(
    clientRegistrationRepository: ClientRegistrationRepository,
    builder: Builder,
  ) = builder.authorisedWebClient(
    authorizedClientManagerUserEnhanced(clientRegistrationRepository),
    "hmpps-x-ray-body-scans-api",
    caseNotesApiBaseUri,
    caseNotesApiTimeout,
  )

  private fun authorizedClientManagerUserEnhanced(clients: ClientRegistrationRepository): OAuth2AuthorizedClientManager {
    val service: OAuth2AuthorizedClientService = InMemoryOAuth2AuthorizedClientService(clients)
    val manager = AuthorizedClientServiceOAuth2AuthorizedClientManager(clients, service)
    val restClientTokenResponseClient = RestClientClientCredentialsTokenResponseClient()
    val authentication = SecurityContextHolder.getContext().authentication

    restClientTokenResponseClient.setParametersCustomizer { parameters ->
      authentication?.name?.let { username ->
        parameters.add("username", username)
      }
    }

    val authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder()
      .clientCredentials { clientCredentialsGrantBuilder: OAuth2AuthorizedClientProviderBuilder.ClientCredentialsGrantBuilder ->
        clientCredentialsGrantBuilder.accessTokenResponseClient(
          restClientTokenResponseClient,
        )
      }
      .build()

    manager.setAuthorizedClientProvider(authorizedClientProvider)
    return manager
  }
}
