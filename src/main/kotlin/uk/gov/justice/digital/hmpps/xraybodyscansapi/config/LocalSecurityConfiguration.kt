package uk.gov.justice.digital.hmpps.xraybodyscansapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.web.SecurityFilterChain

@Configuration
@Profile("dev")
class LocalSecurityConfiguration {

  @Bean
  fun localFilterChain(http: HttpSecurity): SecurityFilterChain {
    http {
      csrf { disable() }
      authorizeHttpRequests { authorize(anyRequest, permitAll) }
    }
    return http.build()
  }
}
