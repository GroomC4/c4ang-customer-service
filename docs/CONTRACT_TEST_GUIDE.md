# Internal API Contract Test 가이드

## 개요

Internal API는 **Spring Cloud Contract**를 사용하여 Consumer-Driven Contract Testing을 수행합니다.

### Contract Test를 사용하는 이유

1. **Avro 제거**: Avro는 REST API JSON 직렬화에 부적합 → DTO 기반으로 전환
2. **명세 검증**: Consumer가 기대하는 API 스펙을 Contract로 정의하고 검증
3. **계약 기반 개발**: Provider(customer-service)가 Contract를 준수하는지 자동 테스트

## Contract 파일 위치

```
customer-api/
└── src/test/resources/contracts/
    └── internal-user-api/
        ├── should_return_user_when_id_exists.yml
        └── should_return_404_when_user_not_found.yml
```

## Contract 예시

### 성공 케이스

```yaml
description: Internal User API - 유효한 사용자 ID로 조회
name: should_return_user_when_id_exists
request:
  method: GET
  url: /internal/v1/users/750e8400-e29b-41d4-a716-446655440001
  headers:
    Content-Type: application/json
response:
  status: 200
  headers:
    Content-Type: application/json
  body:
    userId: "750e8400-e29b-41d4-a716-446655440001"
    username: "고객테스트"
    email: "customer@example.com"
    role: "CUSTOMER"
    isActive: true
    profile:
      fullName: "고객테스트"
      phoneNumber: "010-1111-2222"
      address: null
    createdAt: 1699999999999
    updatedAt: 1699999999999
    lastLoginAt: null
  matchers:
    body:
      - path: $.userId
        type: by_regex
        value: "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
      - path: $.createdAt
        type: by_type
      - path: $.updatedAt
        type: by_type
```

### 실패 케이스 (404)

```yaml
description: Internal User API - 존재하지 않는 사용자 ID로 조회 시 404 반환
name: should_return_404_when_user_not_found
request:
  method: GET
  url: /internal/v1/users/00000000-0000-0000-0000-000000000000
  headers:
    Content-Type: application/json
response:
  status: 404
  headers:
    Content-Type: application/json
  body:
    code: "USER_NOT_FOUND"
    message: "User not found"
  matchers:
    body:
      - path: $.code
        type: by_equality
      - path: $.message
        type: by_type
```

## Consumer 측 사용 방법

다른 서비스(store-service, order-service 등)에서 Internal User API를 호출할 때:

### 1. Contract 의존성 추가

```kotlin
// build.gradle.kts
dependencies {
    testImplementation("org.springframework.cloud:spring-cloud-starter-contract-stub-runner:4.1.4")
}
```

### 2. Contract Test 작성

```kotlin
@SpringBootTest
@AutoConfigureStubRunner(
    ids = ["com.groom:customer-api:+:stubs:8080"],
    stubsMode = StubRunnerProperties.StubsMode.LOCAL
)
class InternalUserApiContractTest {

    @Autowired
    private lateinit var restTemplate: RestTemplate

    @Test
    fun `should call internal user api successfully`() {
        // given
        val userId = "750e8400-e29b-41d4-a716-446655440001"

        // when
        val response = restTemplate.getForEntity(
            "http://localhost:8080/internal/v1/users/$userId",
            UserInternalDto::class.java
        )

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.userId).isEqualTo(userId)
        assertThat(response.body?.username).isEqualTo("고객테스트")
        assertThat(response.body?.email).isEqualTo("customer@example.com")
    }
}
```

## Provider 측 테스트 실행

```bash
# Contract Test 실행
./gradlew :customer-api:test

# Stub JAR 생성 (다른 서비스가 사용할 수 있도록)
./gradlew :customer-api:publishToMavenLocal
```

## Contract 검증 흐름

1. **Consumer**: Contract 파일 작성 (기대하는 API 스펙)
2. **Provider**: Contract 파일을 기반으로 자동 테스트 생성
3. **Provider**: 테스트 실행하여 Contract 준수 여부 확인
4. **Consumer**: Stub Runner로 Provider 스텁을 사용하여 통합 테스트

## 주의사항

- Contract 파일은 **Consumer와 Provider가 함께 협의**하여 작성
- Provider는 **절대 Contract를 임의로 변경하지 않음** (Breaking Change 방지)
- Contract 변경이 필요한 경우 버전 관리 필요

## 참고 문서

- [Spring Cloud Contract 공식 문서](https://spring.io/projects/spring-cloud-contract)
- [Consumer-Driven Contracts 패턴](https://martinfowler.com/articles/consumerDrivenContracts.html)
