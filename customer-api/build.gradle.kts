plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
}

sourceSets {
    test {
        kotlin {
            srcDir("../c4ang-infra/testcontainers/kotlin")
        }
    }
}

dependencies {
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // Spring
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.springframework.retry:spring-retry")
    implementation("com.auth0:java-jwt:4.4.0")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.4.0")

    // Spring Cloud BOM (Spring Boot 3.3.4와 호환)
    implementation(platform("org.springframework.cloud:spring-cloud-dependencies:2023.0.3"))

    // Spring Cloud OpenFeign (버전은 BOM에서 관리)
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")

    // Logging
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.13")

    // Redisson (Redis 클라이언트 with 원자적 연산 지원)
    implementation("org.redisson:redisson-spring-boot-starter:3.24.3")

    // Database
    runtimeOnly("org.postgresql:postgresql")
    implementation("io.hypersistence:hypersistence-utils-hibernate-63:3.7.3")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("io.kotest:kotest-runner-junit5:5.9.1")
    testImplementation("io.kotest:kotest-assertions-core:5.9.1")
    testImplementation("io.mockk:mockk:1.14.5")

    // Karate DSL for E2E Testing
    testImplementation("com.intuit.karate:karate-junit5:1.4.1")
}

tasks.withType<Test> {
    useJUnitPlatform {
        // E2E 테스트는 기본 test 태스크에서 제외 (별도 태스크로 실행)
        excludeTags("e2e-test")
    }

    // 메모리 설정 (통합테스트 Testcontainers 실행을 위해)
    minHeapSize = "512m"
    maxHeapSize = "2048m"

    systemProperty("user.timezone", "KST")
    jvmArgs("--add-opens", "java.base/java.time=ALL-UNNAMED")

    // 테스트 실행 로깅
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }
}

// E2E 테스트 전용 태스크
val e2eTest by tasks.registering(Test::class) {
    description = "Runs E2E tests (Karate scenarios)"
    group = "verification"

    useJUnitPlatform {
        includeTags("e2e-test")
    }

    systemProperty("user.timezone", "KST")
    jvmArgs("--add-opens", "java.base/java.time=ALL-UNNAMED")

    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }

    shouldRunAfter(tasks.test)
}

// Docker Compose 연동 태스크 추가
val dockerComposeUp by tasks.registering(Exec::class) {
    group = "docker"
    description = "Run docker compose up for local infrastructure."
    commandLine(
        "sh",
        "-c",
        "command -v docker >/dev/null 2>&1 && docker compose up -d || echo 'docker not found, skipping docker compose up'",
    )
    workingDir = project.projectDir
}
val dockerComposeDown by tasks.registering(Exec::class) {
    group = "docker"
    description = "Run docker compose down for local infrastructure."
    commandLine(
        "sh",
        "-c",
        "command -v docker >/dev/null 2>&1 && docker compose down || echo 'docker not found, skipping docker compose down'",
    )
    workingDir = project.projectDir
}
tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    dependsOn(dockerComposeUp)
    finalizedBy(dockerComposeDown)
}
