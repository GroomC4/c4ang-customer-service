# Customer Service Internal User API 호출 프롬프트

다른 서비스에서 Customer Service의 Internal User API를 호출하도록 구현할 때 사용할 프롬프트입니다.

---

## 📋 프롬프트 (복사해서 사용하세요)

```
Customer Service의 Internal User API를 호출하는 클라이언트를 구현해주세요.

### 요구사항

1. **의존성 추가**
   - JitPack repository 추가
   - Confluent Maven repository 추가 (Avro 지원)
   - c4ang-contract-hub v1.0.0 의존성 추가
   - Apache Avro 1.11.3 의존성 추가

2. **API Endpoint**
   - URL: `GET /internal/v1/users/{userId}`
   - K8s 내부 통신용 (Service URL: http://customer-service.default.svc.cluster.local:8080)
   - Response: `UserInternalResponse` (Avro 스키마)

3. **구현 방식**
   - Spring Cloud OpenFeign 사용
   - FeignClient 인터페이스 생성
   - Configuration 클래스 작성 (Jackson Decoder/Encoder)
   - 예외 처리 (404 NotFound, 400 BadRequest)

4. **Response Schema (contract-hub Avro)**
   ```kotlin
   UserInternalResponse {
     userId: String (UUID)
     username: String
     email: String
     role: UserRole (enum: CUSTOMER, OWNER, ADMIN)
     isActive: Boolean
     profile: UserProfileInternal {
       fullName: String
       phoneNumber: String
       address: String? (nullable)
     }
     createdAt: Long (epoch millis)
     updatedAt: Long (epoch millis)
     lastLoginAt: Long? (epoch millis, nullable)
   }
   ```

5. **패키지 구조**
   - Feign Client: `adapter.outbound.client.CustomerServiceClient`
   - Configuration: `adapter.outbound.client.CustomerServiceFeignConfig`
   - 사용: Application Service에서 Port로 사용

6. **설정 파일 (application.yml)**
   ```yaml
   feign:
     clients:
       customer-service:
         url: http://customer-service.default.svc.cluster.local:8080
         connect-timeout: 5000
         read-timeout: 5000
   ```

### 참고사항
- contract-hub는 type-safe한 Avro 스키마를 제공합니다
- Timestamp 필드는 epoch milliseconds (Long)로 제공됩니다
- UserRole의 MANAGER, MASTER는 ADMIN으로 매핑됩니다
- 헥사고날 아키텍처 패턴을 따라 구현해주세요 (Port & Adapter)

자세한 구현 예제와 트러블슈팅은 다음 문서를 참고하세요:
/docs/INTERNAL_USER_API_INTEGRATION.md
```

---

## 🎯 더 구체적인 프롬프트 (특정 상황별)

### 1. Store Service에서 사용하는 경우

```
Store Service에서 Customer Service의 Internal User API를 호출하여 점주(Owner) 정보를 조회하는 기능을 구현해주세요.

### 요구사항
1. c4ang-contract-hub v1.0.0 의존성 추가
2. Feign Client 구현 (`StoreServiceCustomerClient`)
3. Port 인터페이스 생성 (`LoadUserPort`)
4. Adapter 구현 (`UserClientAdapter implements LoadUserPort`)
5. 점주 검증 로직:
   - UserRole이 OWNER 또는 ADMIN인 경우만 허용
   - isActive가 true인 경우만 허용
6. 예외 처리:
   - 사용자가 없는 경우: UserNotFoundException
   - 권한이 없는 경우: InsufficientPermissionException

헥사고날 아키텍처 패턴을 따라 구현하고, 도메인 계층에서 Infrastructure에 직접 의존하지 않도록 해주세요.
```

### 2. Order Service에서 사용하는 경우

```
Order Service에서 Customer Service의 Internal User API를 호출하여 주문 시 고객 정보를 검증하는 기능을 구현해주세요.

### 요구사항
1. c4ang-contract-hub v1.0.0 의존성 추가
2. Feign Client 구현 (`OrderServiceCustomerClient`)
3. 주문 생성 시 고객 정보 자동 조회 및 검증:
   - 고객 존재 여부 확인
   - 활성화 상태(isActive) 확인
   - 배송지 정보(profile.address) 확인
4. Caching 적용 (고객 정보는 5분간 캐시)
5. Circuit Breaker 패턴 적용 (Customer Service 장애 시 대응)
6. 비동기 호출 지원 (CompletableFuture)

Resilience4j를 사용하여 장애 격리를 구현하고, 캐시는 Spring Cache Abstraction을 사용해주세요.
```

### 3. Notification Service에서 사용하는 경우

```
Notification Service에서 Customer Service의 Internal User API를 호출하여 알림 발송 시 사용자 연락처 정보를 조회하는 기능을 구현해주세요.

### 요구사항
1. c4ang-contract-hub v1.0.0 의존성 추가
2. Feign Client 구현 (`NotificationServiceCustomerClient`)
3. 사용자 연락처 정보 조회:
   - email (이메일 알림용)
   - profile.phoneNumber (SMS 알림용)
   - profile.fullName (개인화 메시지용)
4. Batch 조회 지원 (여러 사용자 정보를 한 번에 조회)
5. 조회 실패 시 fallback 처리:
   - 기본 이메일/전화번호 사용
   - 재시도 로직 (3회, exponential backoff)
6. 성능 최적화:
   - Redis 캐시 사용
   - 병렬 호출 지원

Kotlin Coroutine을 사용하여 비동기 처리를 구현하고, 캐시 전략을 설명해주세요.
```

---

## 🛠️ 빌드 설정 전용 프롬프트

```
다음 의존성을 프로젝트에 추가해주세요:

1. **Repository 설정**
   - 루트 build.gradle.kts의 allprojects 블록에 추가:
     ```kotlin
     maven { url = uri("https://jitpack.io") }
     maven { url = uri("https://packages.confluent.io/maven/") }
     ```

2. **의존성 추가**
   - 해당 모듈의 build.gradle.kts에 추가:
     ```kotlin
     implementation("com.github.GroomC4:c4ang-contract-hub:v1.0.0")
     implementation("org.apache.avro:avro:1.11.3")
     ```

3. **Feign Client 의존성** (Spring Cloud OpenFeign 사용 시)
   ```kotlin
   implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
   ```

4. **BOM 설정** (Spring Boot 3.3.4 기준)
   ```kotlin
   implementation(platform("org.springframework.cloud:spring-cloud-dependencies:2023.0.3"))
   ```

빌드가 성공하는지 확인하고, ktlint 경고가 있다면 수정해주세요.
```

---

## 📚 추가 참고 프롬프트

### 테스트 코드 작성

```
CustomerServiceClient의 통합 테스트를 작성해주세요.

### 요구사항
1. WireMock을 사용한 API Mock
2. 성공 케이스 테스트:
   - 정상적인 사용자 조회
   - UserRole별 테스트 (CUSTOMER, OWNER, ADMIN)
   - nullable 필드 테스트 (lastLoginAt, address)
3. 실패 케이스 테스트:
   - 404 Not Found
   - 400 Bad Request (잘못된 UUID)
   - 500 Internal Server Error
4. Timeout 테스트
5. Retry 로직 테스트

Kotest + MockK를 사용하여 BDD 스타일로 작성해주세요.
```

### 예외 처리 전략

```
Customer Service 호출 시 발생할 수 있는 예외를 처리하는 전략을 구현해주세요.

### 요구사항
1. FeignException 처리:
   - 404: UserNotFoundException으로 변환
   - 400: InvalidUserIdException으로 변환
   - 500/503: ServiceUnavailableException으로 변환
2. Circuit Breaker 적용 (Resilience4j)
3. Retry 정책:
   - 최대 3회 재시도
   - Exponential backoff (1s, 2s, 4s)
   - 500번대 에러만 재시도
4. Fallback 처리:
   - 캐시된 데이터 반환
   - 기본값 반환
5. 로깅:
   - 에러 로그 (userId, errorCode, message)
   - 메트릭 수집 (호출 횟수, 성공률, 평균 응답시간)

Spring AOP를 사용하여 횡단 관심사로 분리해주세요.
```

---

## ✅ 체크리스트

구현 완료 후 다음 항목을 확인하세요:

- [ ] JitPack repository 추가됨
- [ ] Confluent Maven repository 추가됨
- [ ] contract-hub v1.0.0 의존성 추가됨
- [ ] Apache Avro 1.11.3 의존성 추가됨
- [ ] FeignClient 인터페이스 생성됨
- [ ] Configuration 클래스 작성됨
- [ ] application.yml에 customer-service URL 설정됨
- [ ] 예외 처리 구현됨 (404, 400)
- [ ] Port 인터페이스 정의됨 (헥사고날 아키텍처)
- [ ] Adapter 구현됨
- [ ] 통합 테스트 작성됨
- [ ] 빌드 성공 확인됨
- [ ] ktlint 경고 해결됨

---

## 📞 문의

문제가 발생하거나 추가 도움이 필요한 경우:
1. `/docs/INTERNAL_USER_API_INTEGRATION.md` 문서 확인
2. GitHub Issues: https://github.com/GroomC4/c4ang-customer-service/issues
