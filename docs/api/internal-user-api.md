# Internal User API 명세서

## 개요

K8s 클러스터 내부 서비스 간 통신을 위한 사용자 정보 조회 API입니다.
외부에 노출되지 않으며, 내부 마이크로서비스들이 사용자 정보를 조회할 때 사용합니다.

**Base Path**: `/internal/v1/users`

**Authentication**: 내부 네트워크에서만 접근 가능 (Istio mTLS 필요)

---

## API 엔드포인트

### 사용자 ID로 조회

사용자 고유 ID(UUID)를 사용하여 사용자 정보를 조회합니다.

#### Request

```http
GET /internal/v1/users/{userId}
Content-Type: application/json
```

**Path Parameters**:
- `userId` (UUID, required): 조회할 사용자의 고유 ID

#### Response

**Success (200 OK)**:
```json
{
  "id": "750e8400-e29b-41d4-a716-446655440001",
  "username": "고객테스트",
  "email": "customer@example.com",
  "role": "CUSTOMER",
  "isActive": true,
  "profile": {
    "fullName": "고객테스트",
    "phoneNumber": "010-1111-2222",
    "defaultAddress": "서울시 강남구"
  },
  "lastLoginAt": "2025-11-17T10:30:00",
  "createdAt": "2025-11-10T09:00:00",
  "updatedAt": "2025-11-17T10:30:00"
}
```

**Error Responses**:

- **404 Not Found**: 사용자가 존재하지 않음
  ```json
  {
    "code": "USER_NOT_FOUND",
    "message": "사용자를 찾을 수 없습니다."
  }
  ```

- **400 Bad Request**: 잘못된 UUID 형식
  ```json
  {
    "code": "INVALID_INPUT",
    "message": "유효하지 않은 UUID 형식입니다."
  }
  ```

#### Example

```bash
# 요청 예시
curl -X GET "http://customer-service.default.svc.cluster.local/internal/v1/users/750e8400-e29b-41d4-a716-446655440001" \
  -H "Content-Type: application/json"
```

---

## 응답 모델

### UserInternalResponse

| 필드 | 타입 | 설명 | 예시 |
|------|------|------|------|
| `id` | UUID | 사용자 고유 ID | `"750e8400-e29b-41d4-a716-446655440001"` |
| `username` | String | 사용자명 | `"홍길동"` |
| `email` | String | 이메일 주소 | `"hong@example.com"` |
| `role` | Enum | 사용자 역할 (CUSTOMER, OWNER) | `"CUSTOMER"` |
| `isActive` | Boolean | 활성화 여부 | `true` |
| `profile` | UserProfileInternal | 프로필 정보 (nullable) | 아래 참조 |
| `lastLoginAt` | DateTime | 마지막 로그인 시간 (nullable) | `"2025-11-17T10:30:00"` |
| `createdAt` | DateTime | 생성 시간 | `"2025-11-10T09:00:00"` |
| `updatedAt` | DateTime | 수정 시간 | `"2025-11-17T10:30:00"` |

### UserProfileInternal

| 필드 | 타입 | 설명 | 예시 |
|------|------|------|------|
| `fullName` | String | 전체 이름 | `"홍길동"` |
| `phoneNumber` | String | 전화번호 | `"010-1234-5678"` |
| `defaultAddress` | String | 기본 주소 | `"서울시 강남구"` |

---

## 사용 사례

### 1. 주문 서비스에서 고객 정보 조회

주문 생성 시 고객의 배송 주소를 가져오는 경우:

```kotlin
// Order Service
val userResponse = restTemplate.getForObject(
    "http://customer-service/internal/v1/users/{userId}",
    UserInternalResponse::class.java,
    customerId
)
val shippingAddress = userResponse.profile?.defaultAddress
```

### 2. 알림 서비스에서 사용자 연락처 조회

푸시 알림 발송 시 사용자 정보를 가져오는 경우:

```kotlin
// Notification Service
val userResponse = restTemplate.getForObject(
    "http://customer-service/internal/v1/users/{userId}",
    UserInternalResponse::class.java,
    userId
)
val phoneNumber = userResponse.profile?.phoneNumber
```

### 3. 리뷰 서비스에서 작성자 정보 표시

리뷰 작성자의 사용자명을 표시하는 경우:

```kotlin
// Review Service
val userResponse = restTemplate.getForObject(
    "http://customer-service/internal/v1/users/{userId}",
    UserInternalResponse::class.java,
    reviewAuthorId
)
val authorName = userResponse.username
```

---

## 보안 고려사항

### 1. 네트워크 접근 제어

- 이 API는 K8s 클러스터 내부에서만 접근 가능해야 합니다
- Istio Service Mesh를 통한 mTLS 인증을 사용합니다
- 외부 인터넷에서 직접 접근할 수 없도록 Ingress에서 제외해야 합니다

### 2. 민감 정보 제외

내부 API이지만 다음 정보는 포함하지 않습니다:
- `passwordHash`: 비밀번호 해시
- `refreshToken`: 리프레시 토큰
- 기타 보안 관련 민감 정보

필요한 경우 별도의 인증된 엔드포인트를 통해 제공합니다.

### 3. Rate Limiting

내부 서비스 간 과도한 호출을 방지하기 위해 다음을 고려합니다:
- Circuit Breaker 패턴 적용
- Retry 정책 설정
- Timeout 설정

---

## 성능 고려사항

### 1. 캐싱 전략

자주 조회되는 사용자 정보는 Redis를 통해 캐싱할 수 있습니다:

```kotlin
@Cacheable(value = ["userCache"], key = "#userId")
fun getUserById(userId: UUID): User?
```

캐시 TTL: 5분 (사용자 정보 변경 빈도에 따라 조정)

### 2. 데이터베이스 인덱스

다음 컬럼에 인덱스가 설정되어 있어야 합니다:
- `p_user.id` (Primary Key)
- `p_user.email` (Unique Index)

---

## 버전 관리

- **Current Version**: v1
- **Base Path**: `/internal/v1/users`
- **Breaking Changes**: 메이저 버전 변경 시 새로운 path 생성 (`/internal/v2/users`)
- **Deprecation Policy**: 이전 버전은 최소 6개월 유지 후 제거

---

## 문의 및 지원

- **API Owner**: Customer Service Team
- **Slack Channel**: #customer-service-support
- **Issue Tracker**: [GitHub Issues](https://github.com/your-org/customer-service/issues)
