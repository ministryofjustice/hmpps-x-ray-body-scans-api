@file:Suppress("UnstableApiUsage")

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "11.0.4"
  kotlin("plugin.spring") version "2.4.10"
  kotlin("plugin.jpa") version "2.4.10"
}

dependencies {
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:3.0.0")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-webclient")

  // Monitoring
  implementation("io.sentry:sentry-spring-boot-4:8.51.0")

  // OpenAPI
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.0")
  constraints {
    implementation("org.webjars:swagger-ui:5.32.2")
  }

  // Persistence
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("com.fasterxml.uuid:java-uuid-generator:5.2.0")
  runtimeOnly("org.flywaydb:flyway-database-postgresql")
  runtimeOnly("org.postgresql:postgresql:42.7.13")
  implementation("org.springframework.boot:spring-boot-starter-flyway")
  implementation("com.h2database:h2") // Here in case you want to run locally using h2
  testImplementation("com.h2database:h2") // Tests use h2
  testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")

  testImplementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter-test:3.0.0")
  testImplementation("org.springframework.boot:spring-boot-starter-webflux-test")
  testImplementation("org.wiremock:wiremock-standalone:3.13.2")
  testImplementation("io.swagger.parser.v3:swagger-parser:2.1.46") {
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

  test {
    exclude("**/InitialiseDatabase.class")
  }

  val testSuite = testing.suites.named("test", JvmTestSuite::class)
  register("initialiseDatabase", Test::class) {
    description = "initialise database"
    testClassesDirs = files(testSuite.map { it.sources.output.classesDirs })
    classpath = files(testSuite.map { it.sources.runtimeClasspath })
    include("**/InitialiseDatabase.class")
    systemProperty("spring.profiles.include", "test,schemaspy")
  }

  getByName("initialiseDatabase") {
    onlyIf { gradle.startParameter.taskNames.contains("initialiseDatabase") }
  }
}
