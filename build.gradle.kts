plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "10.5.4"
  kotlin("plugin.spring") version "2.4.0"
  kotlin("plugin.jpa") version "2.4.0"
}

dependencies {
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:2.5.0")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-webclient")

  // Monitoring
  implementation("io.sentry:sentry-spring-boot-4:8.46.0")

  // OpenAPI
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3")
  constraints {
    implementation("org.webjars:swagger-ui:5.32.2")
  }

  // Persistence
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  runtimeOnly("org.flywaydb:flyway-database-postgresql")
  runtimeOnly("org.postgresql:postgresql")
  implementation("org.springframework.boot:spring-boot-starter-flyway")
  implementation("com.h2database:h2") // Here in case you want to run locally using h2
  testImplementation("com.h2database:h2") // Tests use h2
  testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")

  testImplementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter-test:2.5.0")
  testImplementation("org.springframework.boot:spring-boot-starter-webflux-test")
  testImplementation("org.wiremock:wiremock-standalone:3.13.2")
  testImplementation("io.swagger.parser.v3:swagger-parser:2.1.45") {
    exclude(group = "io.swagger.core.v3")
  }
}

kotlin {
  jvmToolchain(25)
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions.jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25
  }
}
