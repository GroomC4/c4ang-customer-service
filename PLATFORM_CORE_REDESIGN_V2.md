# c4ang-platform-core 재설계안 v2

## 1. 핵심 인사이트

### 환경별 실제 차이점

| 항목 | local | test | prod |
|------|-------|------|------|
| **인프라 위치** | Docker (로컬) | Docker (CI/로컬) | AWS RDS, ElastiCache 등 |
| **포트** | 고정 가능 | 동적 필요 (병렬 테스트) | 외부에서 결정 |
| **생명주기** | 개발자가 관리 | 테스트 프레임워크가 관리 | 인프라팀이 관리 |
| **스키마** | DDL 실행 | DDL 실행 | Flyway |

### 핵심 질문에 대한 답

> **Q: local도 동적 포트를 적용하면 분리할 필요가 있나?**

**A: 없다.** 하지만 생명주기 관리 방식이 다르다:
- **local**: 개발자가 `bootRun` 실행 → 앱 종료해도 컨테이너 유지 (재시작 빠름)
- **test**: 테스트 시작 시 컨테이너 시작 → JVM 종료 시 정리

> **Q: Testcontainers Docker Compose 모듈로 대체 가능한가?**

**A: 가능하다.** 그리고 이게 더 나은 선택일 수 있다:
- 현재: 개별 컨테이너 (`PostgreSQLContainer`, `GenericContainer<Redis>`)
- 대안: `DockerComposeContainer`로 통일

---

## 2. 제안: 단일 모듈 + Testcontainers DockerCompose

### 왜 Testcontainers DockerCompose인가?

```
현재 문제점:
─────────────────────────────────────────────────────
local-dev-starter     → 자체 Docker Compose 실행 (ProcessBuilder)
testcontainers-starter → 개별 Testcontainers 사용
                         (PostgreSQLContainer, GenericContainer 등)

결과: 두 환경의 인프라 구성이 다름 (불일치 가능)
```

```
제안:
─────────────────────────────────────────────────────
platform-core → Testcontainers DockerComposeContainer 사용
              → local/test 모두 동일한 docker-compose.yml
              → 동적 포트 할당 자동 지원
              → 컨테이너 공유/재사용 지원

결과: 완전히 동일한 인프라 구성
```

### Testcontainers DockerComposeContainer 장점

1. **동적 포트 할당**: `container.getServicePort("postgres", 5432)`
2. **헬스체크 대기**: `WaitingFor` 전략 지원
3. **컨테이너 재사용**: `withLocalCompose(true)` + Ryuk 비활성화
4. **동일한 docker-compose.yml**: local/test 환경 일치

---

## 3. 모듈 구조

```
platform-core/                          # 단일 모듈
├── src/main/kotlin/com/groom/platform/
│   │
│   ├── PlatformAutoConfiguration.kt    # 메인 진입점
│   ├── PlatformProperties.kt           # 통합 설정
│   │
│   ├── infrastructure/
│   │   ├── DockerComposeInfrastructure.kt   # Testcontainers DockerCompose 래퍼
│   │   ├── InfrastructureLifecycle.kt       # 생명주기 관리
│   │   └── InfrastructureEnvironment.kt     # 동적 포트 → 프로퍼티 주입
│   │
│   ├── datasource/
│   │   ├── DynamicRoutingDataSource.kt
│   │   ├── DataSourceConfiguration.kt
│   │   └── TransactionRoutingAspect.kt
│   │
│   ├── redis/
│   │   └── RedisConfiguration.kt
│   │
│   ├── kafka/
│   │   └── KafkaConfiguration.kt
│   │
│   └── schema/
│       └── SchemaInitializer.kt
│
├── src/main/resources/
│   ├── META-INF/spring/
│   │   └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
│   │
│   └── docker-compose/
│       └── infrastructure.yml           # 공용 docker-compose
│
└── src/test/                            # 테스트용 유틸리티
    └── kotlin/com/groom/platform/test/
        ├── IntegrationTest.kt           # @IntegrationTest 어노테이션
        └── PlatformTestExecutionListener.kt
```

---

## 4. 프로필별 동작

### 공통: Testcontainers DockerComposeContainer

```kotlin
/**
 * local/test 공통으로 사용되는 인프라 관리자
 */
class DockerComposeInfrastructure(
    private val properties: PlatformProperties
) {
    private val compose: DockerComposeContainer<*> by lazy {
        DockerComposeContainer(File("docker-compose/infrastructure.yml"))
            .withServices(getEnabledServices())
            .withExposedService("postgres-primary", 5432, Wait.forHealthcheck())
            .withExposedService("postgres-replica", 5432, Wait.forHealthcheck())
            .withExposedService("redis", 6379, Wait.forHealthcheck())
            .withExposedService("kafka", 9092, Wait.forHealthcheck())
            .withLocalCompose(true)  // 로컬 docker-compose 사용 (더 빠름)
    }

    fun start() {
        compose.start()
    }

    fun stop() {
        compose.stop()
    }

    // 동적 포트 조회
    fun getPostgresPrimaryPort(): Int =
        compose.getServicePort("postgres-primary", 5432)

    fun getPostgresReplicaPort(): Int =
        compose.getServicePort("postgres-replica", 5432)

    fun getRedisPort(): Int =
        compose.getServicePort("redis", 6379)

    fun getKafkaPort(): Int =
        compose.getServicePort("kafka", 9092)

    private fun getEnabledServices(): List<String> {
        return buildList {
            if (properties.infrastructure.postgres.enabled) {
                add("postgres-primary")
                add("postgres-replica")
            }
            if (properties.infrastructure.redis.enabled) add("redis")
            if (properties.infrastructure.kafka.enabled) add("kafka")
        }
    }
}
```

### local 프로필: 컨테이너 유지

```kotlin
@Configuration
@Profile("local")
class LocalInfraConfiguration(
    private val properties: PlatformProperties
) {
    companion object {
        // JVM 전역 싱글톤 - 앱 재시작해도 컨테이너 유지
        @Volatile
        private var sharedInfra: DockerComposeInfrastructure? = null

        @Synchronized
        fun getOrCreate(properties: PlatformProperties): DockerComposeInfrastructure {
            return sharedInfra ?: DockerComposeInfrastructure(properties).also {
                it.start()
                sharedInfra = it
                // JVM 종료 시에도 컨테이너 유지 (Ryuk 비활성화)
                // 개발자가 수동으로 docker-compose down 해야 함
            }
        }
    }

    @Bean
    fun dockerComposeInfrastructure(): DockerComposeInfrastructure {
        return getOrCreate(properties)
    }
}
```

### test 프로필: 테스트 후 정리

```kotlin
@Configuration
@Profile("test")
class TestInfraConfiguration(
    private val properties: PlatformProperties
) {
    companion object {
        // JVM 전역 싱글톤 - 테스트 간 공유
        @Volatile
        private var sharedInfra: DockerComposeInfrastructure? = null

        @Synchronized
        fun getOrCreate(properties: PlatformProperties): DockerComposeInfrastructure {
            return sharedInfra ?: DockerComposeInfrastructure(properties).also {
                it.start()
                sharedInfra = it
                // JVM 종료 시 정리 (Ryuk이 처리)
            }
        }
    }

    @Bean
    fun dockerComposeInfrastructure(): DockerComposeInfrastructure {
        return getOrCreate(properties)
    }
}
```

### prod 프로필: 인프라 관리 안함

```kotlin
@Configuration
@Profile("prod")
class ProdConfiguration {
    // DockerComposeInfrastructure 빈 없음
    // DataSource는 application-prod.yml 설정 사용
}
```

---

## 5. 프로퍼티 자동 주입

```kotlin
/**
 * DockerComposeInfrastructure의 동적 포트를 Spring 프로퍼티로 주입
 */
@Configuration
@Profile("local | test")
class InfrastructureEnvironmentConfiguration(
    private val infra: DockerComposeInfrastructure,
    private val environment: ConfigurableEnvironment
) {
    @PostConstruct
    fun injectProperties() {
        val props = mapOf(
            // PostgreSQL
            "spring.datasource.master.url" to
                "jdbc:postgresql://localhost:${infra.getPostgresPrimaryPort()}/groom",
            "spring.datasource.master.username" to "application",
            "spring.datasource.master.password" to "application",

            "spring.datasource.replica.url" to
                "jdbc:postgresql://localhost:${infra.getPostgresReplicaPort()}/groom",
            "spring.datasource.replica.username" to "application",
            "spring.datasource.replica.password" to "application",

            // Redis
            "spring.data.redis.host" to "localhost",
            "spring.data.redis.port" to infra.getRedisPort().toString(),

            // Kafka
            "spring.kafka.bootstrap-servers" to "localhost:${infra.getKafkaPort()}"
        )

        environment.propertySources.addFirst(
            MapPropertySource("dockerComposeInfra", props)
        )
    }
}
```

---

## 6. DataSource 구성 (프로필 무관)

```kotlin
@AutoConfiguration
@ConditionalOnClass(DataSource::class)
class DataSourceConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "spring.datasource.master", name = ["url"])
    fun masterDataSource(
        @Value("\${spring.datasource.master.url}") url: String,
        @Value("\${spring.datasource.master.username}") username: String,
        @Value("\${spring.datasource.master.password}") password: String
    ): DataSource {
        return HikariDataSource().apply {
            jdbcUrl = url
            this.username = username
            this.password = password
            poolName = "Master-Pool"
        }
    }

    @Bean
    @ConditionalOnProperty(prefix = "spring.datasource.replica", name = ["url"])
    fun replicaDataSource(
        @Value("\${spring.datasource.replica.url}") url: String,
        @Value("\${spring.datasource.replica.username}") username: String,
        @Value("\${spring.datasource.replica.password}") password: String
    ): DataSource {
        return HikariDataSource().apply {
            jdbcUrl = url
            this.username = username
            this.password = password
            poolName = "Replica-Pool"
        }
    }

    @Bean
    fun routingDataSource(
        @Qualifier("masterDataSource") master: DataSource,
        @Qualifier("replicaDataSource") replica: DataSource
    ): DynamicRoutingDataSource {
        return DynamicRoutingDataSource().apply {
            setTargetDataSources(mapOf(
                DataSourceType.MASTER to master,
                DataSourceType.REPLICA to replica
            ))
            setDefaultTargetDataSource(master)
        }
    }

    @Primary
    @Bean
    fun dataSource(routingDataSource: DynamicRoutingDataSource): DataSource {
        return LazyConnectionDataSourceProxy(routingDataSource)
    }
}
```

---

## 7. 도메인 서비스 사용 예시

### build.gradle.kts

```kotlin
dependencies {
    implementation("com.groom.platform:platform-core:2.0.0")
}
```

### application.yml (공통)

```yaml
spring:
  application:
    name: customer-api
```

### application-local.yml

```yaml
# 대부분 기본값 사용, 필요시 커스터마이징
platform:
  infrastructure:
    postgres:
      enabled: true
      database: customer-db  # 기본값: groom
    redis:
      enabled: true
    kafka:
      enabled: false  # 이 서비스는 Kafka 안 씀
```

### application-test.yml

```yaml
# local과 동일한 설정 체계
platform:
  infrastructure:
    postgres:
      enabled: true
    redis:
      enabled: true

  schema:
    locations:
      - classpath:sql/schema.sql
```

### application-prod.yml

```yaml
spring:
  datasource:
    master:
      url: ${DB_MASTER_URL}
      username: ${DB_USERNAME}
      password: ${DB_PASSWORD}
    replica:
      url: ${DB_REPLICA_URL}
      username: ${DB_USERNAME}
      password: ${DB_PASSWORD}

  data:
    redis:
      host: ${REDIS_HOST}
      port: ${REDIS_PORT}

platform:
  infrastructure:
    # prod에서는 인프라 자동 시작 안함
    docker-compose-enabled: false

  schema:
    init-enabled: false  # Flyway 사용
```

---

## 8. docker-compose/infrastructure.yml

```yaml
version: '3.8'

services:
  postgres-primary:
    image: postgres:17-alpine
    environment:
      POSTGRES_USER: application
      POSTGRES_PASSWORD: application
      POSTGRES_DB: groom
    ports:
      - "5432"  # 동적 포트 할당
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U application -d groom"]
      interval: 5s
      timeout: 3s
      retries: 10

  postgres-replica:
    image: postgres:17-alpine
    environment:
      POSTGRES_USER: application
      POSTGRES_PASSWORD: application
      POSTGRES_DB: groom
    ports:
      - "5432"  # 동적 포트 할당
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U application -d groom"]
      interval: 5s
      timeout: 3s
      retries: 10

  redis:
    image: redis:7-alpine
    ports:
      - "6379"  # 동적 포트 할당
    command: ["redis-server", "--appendonly", "yes"]
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s
      timeout: 3s
      retries: 10

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:9093
      KAFKA_LISTENERS: PLAINTEXT://:9092,CONTROLLER://:9093
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,CONTROLLER:PLAINTEXT
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"
      CLUSTER_ID: MkU3OEVBNTcwNTJENDM2Qk
    ports:
      - "9092"  # 동적 포트 할당
    healthcheck:
      test: ["CMD", "kafka-broker-api-versions", "--bootstrap-server", "localhost:9092"]
      interval: 10s
      timeout: 10s
      retries: 10
```

---

## 9. 기존 구조 vs 새 구조

| 항목 | 기존 | 새 구조 |
|------|------|---------|
| **모듈 수** | 3개 (충돌 발생) | 1개 |
| **local 인프라** | ProcessBuilder로 docker-compose | Testcontainers DockerCompose |
| **test 인프라** | 개별 Testcontainers | Testcontainers DockerCompose |
| **포트** | local 고정 / test 동적 | 둘 다 동적 |
| **docker-compose** | 2개 (local용, test용) | 1개 (공용) |
| **DataSource 생성** | 3곳에서 중복 | 1곳에서 통합 |
| **@Primary 충돌** | 있음 | 없음 |

---

## 10. 고려사항

### Testcontainers DockerCompose 제약사항

1. **의존성 추가 필요**
   ```kotlin
   implementation("org.testcontainers:testcontainers")
   // 또는 더 가벼운 docker-compose 모듈만
   ```

2. **prod 환경 배포 시**
   - Testcontainers 의존성이 포함되지만 사용되지 않음
   - `compileOnly`로 분리하거나 런타임에 무시

3. **local 환경 컨테이너 유지**
   - Ryuk(자동 정리) 비활성화 필요: `TESTCONTAINERS_RYUK_DISABLED=true`
   - 또는 `withLocalCompose(true)` 사용 시 Ryuk 우회

### 대안: local만 ProcessBuilder 유지

```
local  → 기존처럼 ProcessBuilder로 docker-compose (고정 포트)
test   → Testcontainers DockerCompose (동적 포트)
```

이 경우 docker-compose.yml 2개 유지해야 하지만, prod 배포 시 Testcontainers 의존성 제외 가능

---

## 11. 결론

### 권장안: 단일 모듈 + Testcontainers DockerCompose

**장점:**
- local/test 환경 완전 일치
- 동적 포트로 충돌 없음
- 단일 docker-compose.yml 관리
- @Primary 충돌 완전 해결

**단점:**
- prod에 Testcontainers 의존성 포함 (사용 안 함)
- local에서 Ryuk 비활성화 필요

### 구현 순서

1. `platform-core` 모듈 신규 생성
2. `DockerComposeInfrastructure` 구현
3. `DataSourceConfiguration` 통합
4. 기존 3개 모듈 → deprecated
5. 도메인 서비스 마이그레이션

---

## 12. 추가 질문

1. **prod 배포 시 Testcontainers 의존성 포함되는 것이 괜찮은가?**
   - 실제로 사용되지 않음 (조건부 빈)
   - JAR 크기 약간 증가

2. **local 환경에서 컨테이너를 유지할 때 Ryuk 비활성화 방식은?**
   - 환경변수: `TESTCONTAINERS_RYUK_DISABLED=true`
   - 또는 `.testcontainers.properties` 파일

3. **Schema 초기화는 어느 시점에?**
   - 컨테이너 시작 후, DataSource 생성 전
   - `docker-entrypoint-initdb.d` 활용 또는 Spring 초기화
