# Contract Test Consumer 가이드

이 문서는 Customer Service의 Internal API를 호출하는 Consumer 서비스에서 Contract Test를 구현하는 방법을 설명합니다.

## 개요

Customer Service는 **Provider**로서 Contract Stub을 GitHub Packages에 발행합니다.
Consumer 서비스는 이 Stub을 사용하여 실제 Customer Service 없이도 Contract Test를 수행할 수 있습니다.

## Consumer 측 설정

### 1. 의존성 추가

**build.gradle.kts**:
```kotlin
plugins {
    id("org.springframework.cloud.contract") version "4.1.4"
}

dependencies {
    // Spring Cloud Contract Stub Runner
    testImplementation("org.springframework.cloud:spring-cloud-starter-contract-stub-runner")
}

repositories {
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/GroomC4/c4ang-customer-service")
        credentials {
            username = System.getenv("GITHUB_ACTOR") ?: project.findProperty("gpr.user") as String?
            password = System.getenv("GITHUB_TOKEN") ?: project.findProperty("gpr.key") as String?
        }
    }
}
```

### 2. Contract Test 작성

**예시: Store Service에서 Customer Internal API를 호출하는 경우**

```kotlin
package com.groom.store.client

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties
import org.springframework.web.client.RestTemplate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@AutoConfigureStubRunner(
    ids = ["com.groom:customer-service-contract-stubs:+:stubs:8080"],
    stubsMode = StubRunnerProperties.StubsMode.REMOTE,
    repositoryRoot = "https://maven.pkg.github.com/GroomC4/c4ang-customer-service"
)
class CustomerServiceClientTest {

    @Autowired
    private lateinit var restTemplate: RestTemplate

    @Test
    fun `should get user by id from customer service`() {
        // given
        val userId = "750e8400-e29b-41d4-a716-446655440001"

        // when
        val response = restTemplate.getForObject(
            "http://localhost:8080/internal/v1/users/$userId",
            UserInternalResponse::class.java
        )

        // then
        assertNotNull(response)
        assertEquals(userId, response.userId)
        assertEquals("고객테스트", response.username)
        assertEquals("customer@example.com", response.email)
        assertEquals("CUSTOMER", response.role)
        assertEquals(true, response.isActive)
    }

    @Test
    fun `should return 404 when user not found`() {
        // given
        val nonExistentUserId = "00000000-0000-0000-0000-000000000000"

        // when & then
        try {
            restTemplate.getForObject(
                "http://localhost:8080/internal/v1/users/$nonExistentUserId",
                UserInternalResponse::class.java
            )
            fail("Expected exception")
        } catch (e: HttpClientErrorException) {
            assertEquals(404, e.statusCode.value())
        }
    }
}

data class UserInternalResponse(
    val userId: String,
    val username: String,
    val email: String,
    val role: String,
    val isActive: Boolean,
    val profile: UserProfileResponse,
    val createdAt: Long,
    val updatedAt: Long,
    val lastLoginAt: Long?
)

data class UserProfileResponse(
    val fullName: String,
    val phoneNumber: String,
    val address: String?
)
```

### 3. Feign Client 사용 시

```kotlin
package com.groom.store.client

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

@FeignClient(
    name = "customer-service",
    url = "\${customer-service.url:http://localhost:8080}"
)
interface CustomerServiceClient {

    @GetMapping("/internal/v1/users/{userId}")
    fun getUserById(@PathVariable userId: String): UserInternalResponse
}
```

**Contract Test**:
```kotlin
@SpringBootTest
@AutoConfigureStubRunner(
    ids = ["com.groom:customer-service-contract-stubs:+:stubs"],
    stubsMode = StubRunnerProperties.StubsMode.REMOTE
)
class CustomerServiceClientContractTest {

    @Autowired
    private lateinit var customerServiceClient: CustomerServiceClient

    @Test
    fun `feign client should work with contract stub`() {
        // when
        val user = customerServiceClient.getUserById("750e8400-e29b-41d4-a716-446655440001")

        // then
        assertEquals("고객테스트", user.username)
        assertEquals("CUSTOMER", user.role)
    }
}
```

## 사용 가능한 Contract Stubs

### 발행 위치
- **Group ID**: `com.groom`
- **Artifact ID**: `customer-service-contract-stubs`
- **Repository**: `https://maven.pkg.github.com/GroomC4/c4ang-customer-service`

### 버전 관리
- Git Tag를 푸시할 때마다 해당 버전의 Contract Stub이 발행됩니다.
- 예: `v1.0.0` 태그 → `customer-service-contract-stubs:1.0.0`

### 현재 제공되는 Contract

#### 1. Internal User API - 사용자 조회 (성공)
- **URL**: `GET /internal/v1/users/{userId}`
- **Request**: `userId = "750e8400-e29b-41d4-a716-446655440001"`
- **Response**: 200 OK
```json
{
  "userId": "750e8400-e29b-41d4-a716-446655440001",
  "username": "고객테스트",
  "email": "customer@example.com",
  "role": "CUSTOMER",
  "isActive": true,
  "profile": {
    "fullName": "고객테스트",
    "phoneNumber": "010-1111-2222",
    "address": null
  },
  "createdAt": 1699999999999,
  "updatedAt": 1699999999999,
  "lastLoginAt": null
}
```

#### 2. Internal User API - 사용자 미존재 (404)
- **URL**: `GET /internal/v1/users/{userId}`
- **Request**: `userId = "00000000-0000-0000-0000-000000000000"`
- **Response**: 404 Not Found
```json
{
  "code": "USER_NOT_FOUND",
  "message": "User not found"
}
```

## 환경 변수 설정

Contract Stub을 다운로드하려면 GitHub Packages 인증이 필요합니다.

### Local 개발 환경
```bash
export GITHUB_ACTOR=your-github-username
export GITHUB_TOKEN=your-github-token
```

또는 `~/.gradle/gradle.properties`:
```properties
gpr.user=your-github-username
gpr.key=your-github-token
```

### GitHub Actions
```yaml
- name: Run Contract Tests
  env:
    GITHUB_ACTOR: ${{ github.actor }}
    GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
  run: ./gradlew test
```

## 참고자료

- [Spring Cloud Contract Documentation](https://spring.io/projects/spring-cloud-contract)
- [Consumer Side Contract Testing](https://cloud.spring.io/spring-cloud-contract/reference/html/project-features.html#features-stub-runner)
- [GitHub Packages Maven](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry)

## 문의

Contract 추가 요청이나 문제 발생 시 Customer Service 팀에 문의하세요.
