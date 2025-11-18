# Internal User API 통합 가이드

이 문서는 Customer Service의 Internal User API를 호출하는 다른 마이크로서비스에서 contract-hub를 사용하는 방법을 설명합니다.

## 개요

Customer Service는 K8s 내부 마이크로서비스 간 통신을 위한 Internal User API를 제공합니다. 이 API는 [c4ang-contract-hub](https://github.com/GroomC4/c4ang-contract-hub)의 Avro 스키마를 사용하여 type-safe한 통신을 보장합니다.

## 1. 의존성 추가

### Gradle (Kotlin DSL)

**1.1. Repository 추가**

루트 `build.gradle.kts` 파일의 `allprojects` 또는 해당 모듈의 `repositories` 블록에 JitPack을 추가하세요:

```kotlin
allprojects {
    repositories {
        mavenCentral()

        // JitPack for contract-hub
        maven { url = uri("https://jitpack.io") }

        // Confluent for Kafka Avro Serializer (Avro 의존성)
        maven { url = uri("https://packages.confluent.io/maven/") }
    }
}
```

**1.2. 의존성 추가**

해당 서비스 모듈의 `build.gradle.kts`에 다음 의존성을 추가하세요:

```kotlin
dependencies {
    // Contract Hub: API 스키마 (최신 버전 확인: https://jitpack.io/#GroomC4/c4ang-contract-hub)
    implementation("com.github.GroomC4:c4ang-contract-hub:v1.0.0")

    // Apache Avro (contract-hub가 사용하는 Avro 클래스용)
    implementation("org.apache.avro:avro:1.11.3")
}
```

### Gradle (Groovy DSL)

```groovy
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
    maven { url 'https://packages.confluent.io/maven/' }
}

dependencies {
    implementation 'com.github.GroomC4:c4ang-contract-hub:v1.0.0'
    implementation 'org.apache.avro:avro:1.11.3'
}
```

### Maven

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
    <repository>
        <id>confluent</id>
        <url>https://packages.confluent.io/maven/</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.GroomC4</groupId>
        <artifactId>c4ang-contract-hub</artifactId>
        <version>v1.0.0</version>
    </dependency>
    <dependency>
        <groupId>org.apache.avro</groupId>
        <artifactId>avro</artifactId>
        <version>1.11.3</version>
    </dependency>
</dependencies>
```

## 2. API 스펙

### 2.1. Endpoint

```
GET /internal/v1/users/{userId}
```

- **Method**: GET
- **Path Parameter**:
  - `userId` (String, UUID): 조회할 사용자 ID
- **Response**: `UserInternalResponse` (Avro 스키마)
- **Status Codes**:
  - `200 OK`: 성공
  - `404 NOT_FOUND`: 사용자를 찾을 수 없음
  - `400 BAD_REQUEST`: 잘못된 UUID 형식

### 2.2. Response Schema (Avro)

**UserInternalResponse**:
```json
{
  "type": "record",
  "name": "UserInternalResponse",
  "namespace": "com.groom.ecommerce.customer.api.avro",
  "fields": [
    {"name": "userId", "type": "string"},
    {"name": "username", "type": "string"},
    {"name": "email", "type": "string"},
    {"name": "role", "type": {"type": "enum", "name": "UserRole", "symbols": ["CUSTOMER", "OWNER", "ADMIN"]}},
    {"name": "isActive", "type": "boolean"},
    {"name": "profile", "type": "UserProfileInternal"},
    {"name": "createdAt", "type": "long"},
    {"name": "updatedAt", "type": "long"},
    {"name": "lastLoginAt", "type": ["null", "long"], "default": null}
  ]
}
```

**UserProfileInternal**:
```json
{
  "type": "record",
  "name": "UserProfileInternal",
  "namespace": "com.groom.ecommerce.customer.api.avro",
  "fields": [
    {"name": "fullName", "type": "string"},
    {"name": "phoneNumber", "type": "string"},
    {"name": "address", "type": ["null", "string"], "default": null}
  ]
}
```

**UserRole** (enum):
- `CUSTOMER`: 일반 고객
- `OWNER`: 점주
- `ADMIN`: 관리자 (Customer Service의 MANAGER, MASTER 역할이 매핑됨)

**Timestamp 필드**:
- `createdAt`, `updatedAt`: epoch milliseconds (long)
- `lastLoginAt`: epoch milliseconds (long, nullable)

## 3. 구현 예제

### 3.1. Spring Boot + Kotlin (OpenFeign 사용)

**Feign Client 정의**:

```kotlin
package com.example.yourservice.adapter.outbound.client

import com.groom.ecommerce.customer.api.avro.UserInternalResponse
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import java.util.UUID

@FeignClient(
    name = "customer-service",
    url = "\${feign.clients.customer-service.url}",
    configuration = [CustomerServiceFeignConfig::class],
)
interface CustomerServiceClient {
    @GetMapping("/internal/v1/users/{userId}")
    fun getUserById(@PathVariable userId: UUID): UserInternalResponse
}
```

**Configuration**:

```kotlin
package com.example.yourservice.adapter.outbound.client

import feign.codec.Decoder
import feign.codec.Encoder
import feign.jackson.JacksonDecoder
import feign.jackson.JacksonEncoder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CustomerServiceFeignConfig {
    @Bean
    fun feignDecoder(): Decoder = JacksonDecoder()

    @Bean
    fun feignEncoder(): Encoder = JacksonEncoder()
}
```

**application.yml**:

```yaml
feign:
  clients:
    customer-service:
      url: http://customer-service.default.svc.cluster.local:8080
      connect-timeout: 5000
      read-timeout: 5000
```

**사용 예제**:

```kotlin
package com.example.yourservice.application.service

import com.example.yourservice.adapter.outbound.client.CustomerServiceClient
import com.groom.ecommerce.customer.api.avro.UserInternalResponse
import com.groom.ecommerce.customer.api.avro.UserRole
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

@Service
class YourService(
    private val customerServiceClient: CustomerServiceClient,
) {
    fun processUserData(userId: UUID) {
        // 1. Customer Service에서 사용자 정보 조회
        val userResponse: UserInternalResponse = customerServiceClient.getUserById(userId)

        // 2. Avro 스키마 데이터 사용
        println("User ID: ${userResponse.userId}")
        println("Username: ${userResponse.username}")
        println("Email: ${userResponse.email}")
        println("Role: ${userResponse.role}") // UserRole enum
        println("Is Active: ${userResponse.isActive}")

        // 3. Profile 정보 접근
        val profile = userResponse.profile
        println("Full Name: ${profile.fullName}")
        println("Phone: ${profile.phoneNumber}")
        println("Address: ${profile.address ?: "N/A"}")

        // 4. Timestamp 변환 (epoch millis → LocalDateTime)
        val createdAt = epochMillisToLocalDateTime(userResponse.createdAt)
        val updatedAt = epochMillisToLocalDateTime(userResponse.updatedAt)
        val lastLoginAt = userResponse.lastLoginAt?.let { epochMillisToLocalDateTime(it) }

        println("Created At: $createdAt")
        println("Updated At: $updatedAt")
        println("Last Login At: ${lastLoginAt ?: "Never"}")

        // 5. Role별 분기 처리
        when (userResponse.role) {
            UserRole.CUSTOMER -> handleCustomer(userResponse)
            UserRole.OWNER -> handleOwner(userResponse)
            UserRole.ADMIN -> handleAdmin(userResponse)
        }
    }

    private fun epochMillisToLocalDateTime(epochMillis: Long): LocalDateTime =
        Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()

    private fun handleCustomer(user: UserInternalResponse) {
        // Customer 전용 로직
    }

    private fun handleOwner(user: UserInternalResponse) {
        // Owner 전용 로직
    }

    private fun handleAdmin(user: UserInternalResponse) {
        // Admin 전용 로직
    }
}
```

### 3.2. Spring Boot + Java (RestTemplate 사용)

```java
package com.example.yourservice.adapter.outbound.client;

import com.groom.ecommerce.customer.api.avro.UserInternalResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Component
public class CustomerServiceClient {
    private final RestTemplate restTemplate;
    private final String customerServiceUrl;

    public CustomerServiceClient(
        RestTemplate restTemplate,
        @Value("${customer-service.url}") String customerServiceUrl
    ) {
        this.restTemplate = restTemplate;
        this.customerServiceUrl = customerServiceUrl;
    }

    public UserInternalResponse getUserById(UUID userId) {
        String url = customerServiceUrl + "/internal/v1/users/" + userId;
        return restTemplate.getForObject(url, UserInternalResponse.class);
    }
}
```

### 3.3. 예외 처리

```kotlin
package com.example.yourservice.application.service

import com.example.yourservice.adapter.outbound.client.CustomerServiceClient
import com.groom.ecommerce.customer.api.avro.UserInternalResponse
import feign.FeignException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class YourService(
    private val customerServiceClient: CustomerServiceClient,
) {
    fun getUserSafely(userId: UUID): UserInternalResponse? {
        return try {
            customerServiceClient.getUserById(userId)
        } catch (e: FeignException.NotFound) {
            // 404: 사용자를 찾을 수 없음
            println("User not found: $userId")
            null
        } catch (e: FeignException.BadRequest) {
            // 400: 잘못된 UUID 형식
            println("Invalid UUID format: $userId")
            null
        } catch (e: FeignException) {
            // 기타 Feign 예외
            println("Feign error: ${e.status()} - ${e.message}")
            throw e
        }
    }
}
```

## 4. 테스트

### 4.1. WireMock을 사용한 Integration Test

```kotlin
package com.example.yourservice.adapter.outbound.client

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.groom.ecommerce.customer.api.avro.UserInternalResponse
import com.groom.ecommerce.customer.api.avro.UserProfileInternal
import com.groom.ecommerce.customer.api.avro.UserRole
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class CustomerServiceClientTest {
    private lateinit var wireMockServer: WireMockServer
    private lateinit var customerServiceClient: CustomerServiceClient

    @BeforeEach
    fun setUp() {
        wireMockServer = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
        wireMockServer.start()

        // Configure Feign client with WireMock URL
        // customerServiceClient = ... (실제 Feign client 초기화)
    }

    @AfterEach
    fun tearDown() {
        wireMockServer.stop()
    }

    @Test
    fun `getUserById should return user data`() {
        // given
        val userId = UUID.randomUUID()
        val mockResponse = """
        {
          "userId": "$userId",
          "username": "testuser",
          "email": "test@example.com",
          "role": "CUSTOMER",
          "isActive": true,
          "profile": {
            "fullName": "Test User",
            "phoneNumber": "010-1234-5678",
            "address": null
          },
          "createdAt": 1704067200000,
          "updatedAt": 1704067200000,
          "lastLoginAt": 1704067200000
        }
        """.trimIndent()

        wireMockServer.stubFor(
            get(urlEqualTo("/internal/v1/users/$userId"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(mockResponse)
                )
        )

        // when
        val result = customerServiceClient.getUserById(userId)

        // then
        assert(result.userId == userId.toString())
        assert(result.username == "testuser")
        assert(result.role == UserRole.CUSTOMER)
    }
}
```

## 5. 버전 관리

### 5.1. contract-hub 버전 업데이트

contract-hub가 업데이트되면 다음과 같이 버전을 변경하세요:

```kotlin
dependencies {
    // 새 버전으로 업데이트
    implementation("com.github.GroomC4:c4ang-contract-hub:v1.1.0")
}
```

### 5.2. Breaking Changes 확인

contract-hub의 릴리즈 노트를 확인하여 Breaking Changes가 있는지 확인하세요:
https://github.com/GroomC4/c4ang-contract-hub/releases

## 6. 트러블슈팅

### 6.1. JitPack 빌드 실패

**증상**: `Could not resolve com.github.GroomC4:c4ang-contract-hub:v1.0.0`

**해결책**:
1. JitPack 빌드 상태 확인: https://jitpack.io/#GroomC4/c4ang-contract-hub/v1.0.0
2. 빌드가 실패한 경우, 로그를 확인하여 원인 파악
3. GitHub 토큰이 필요한 경우 설정:
   ```kotlin
   maven {
       url = uri("https://jitpack.io")
       credentials { username = System.getenv("JITPACK_TOKEN") }
   }
   ```

### 6.2. Avro 직렬화/역직렬화 오류

**증상**: `ClassNotFoundException: org.apache.avro.specific.SpecificRecordBase`

**해결책**:
```kotlin
// Apache Avro 의존성 추가
implementation("org.apache.avro:avro:1.11.3")
```

### 6.3. Confluent Repository 404

**증상**: `Could not resolve io.confluent:kafka-avro-serializer`

**해결책**:
```kotlin
repositories {
    maven { url = uri("https://packages.confluent.io/maven/") }
}
```

## 7. 참고 자료

- [c4ang-contract-hub GitHub](https://github.com/GroomC4/c4ang-contract-hub)
- [JitPack 문서](https://jitpack.io/docs/)
- [Apache Avro 문서](https://avro.apache.org/docs/current/)
- [Spring Cloud OpenFeign](https://docs.spring.io/spring-cloud-openfeign/docs/current/reference/html/)

## 8. 문의

Internal User API 통합 관련 문의사항은 다음으로 연락 주세요:
- GitHub Issues: https://github.com/GroomC4/c4ang-customer-service/issues
- Team Channel: [팀 채널 정보]
