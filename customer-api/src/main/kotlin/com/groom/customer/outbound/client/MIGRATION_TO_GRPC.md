# gRPC 전환 가이드

StoreClient 인터페이스를 통해 REST API에서 gRPC로 전환하는 방법을 설명합니다.

## 현재 구조

```
StoreClient (interface)
    ↑
    └── StoreFeignClient (REST 구현체, @FeignClient)
            ↑
            └── StoreClientAdapter가 주입받음
```

## 전환 시나리오

### 방법 1: @Primary 사용 (권장)

gRPC 구현체에 `@Primary`를 붙여 기본 구현체로 지정합니다.

```kotlin
// 1. gRPC 구현체 추가
@Component
@Primary  // 기본 구현체로 지정
class StoreGrpcClient(
    private val storeServiceStub: StoreServiceBlockingStub
) : StoreClient {

    override fun getStore(storeId: Long): StoreResponse {
        val request = GetStoreRequest.newBuilder()
            .setStoreId(storeId)
            .build()

        val response = storeServiceStub.getStore(request)

        return StoreResponse(
            id = response.id,
            name = response.name,
            description = response.description,
            address = response.address,
            phoneNumber = response.phoneNumber,
            ownerId = response.ownerId,
            status = StoreStatus.valueOf(response.status)
        )
    }

    override fun getStores(page: Int, size: Int): StoreListResponse {
        // gRPC 구현
    }

    override fun getStoreProducts(storeId: Long, page: Int, size: Int): ProductListResponse {
        // gRPC 구현
    }
}

// 2. REST 구현체는 그대로 유지 (백업용)
@FeignClient(
    name = "store-service",
    url = "\${feign.clients.store-service.url}"
)
interface StoreFeignClient : StoreClient {
    // 기존 코드 그대로
}

// 3. Adapter는 수정 불필요!
@Component
class StoreClientAdapter(
    private val storeClient: StoreClient  // @Primary가 붙은 StoreGrpcClient 주입됨
) {
    // 코드 변경 없음!
}
```

### 방법 2: @Qualifier 사용

명시적으로 어떤 구현체를 사용할지 지정합니다.

```kotlin
// 1. gRPC 구현체 추가
@Component("storeGrpcClient")
class StoreGrpcClient(...) : StoreClient {
    // 구현
}

// 2. REST 구현체 이름 지정
@FeignClient(
    name = "store-service",
    url = "\${feign.clients.store-service.url}",
    qualifiers = ["storeFeignClient"]  // 명시적 이름
)
interface StoreFeignClient : StoreClient {
    // 기존 코드
}

// 3. Adapter에서 선택
@Component
class StoreClientAdapter(
    @Qualifier("storeGrpcClient")  // 명시적으로 gRPC 선택
    private val storeClient: StoreClient
) {
    // 코드 변경
}
```

### 방법 3: @Profile 사용 (환경별 전환)

프로파일에 따라 자동으로 구현체를 선택합니다.

```kotlin
// 1. gRPC 구현체 (프로파일: grpc)
@Component
@Profile("grpc")
class StoreGrpcClient(...) : StoreClient {
    // 구현
}

// 2. REST 구현체 (프로파일: rest)
// StoreFeignClient.kt
@FeignClient(...)
@Profile("rest")  // REST 프로파일에서만 활성화
interface StoreFeignClient : StoreClient {
    // 기존 코드
}

// 3. Adapter는 수정 불필요!
@Component
class StoreClientAdapter(
    private val storeClient: StoreClient  // 프로파일에 따라 자동 주입
) {
    // 코드 변경 없음!
}

// 4. application.yml 설정
spring:
  profiles:
    active: grpc  # 또는 rest
```

### 방법 4: @ConditionalOnProperty 사용

프로퍼티 값에 따라 구현체를 선택합니다.

```kotlin
// 1. gRPC 구현체
@Component
@ConditionalOnProperty(name = "store.client.type", havingValue = "grpc")
class StoreGrpcClient(...) : StoreClient {
    // 구현
}

// 2. REST 구현체
@Configuration
@ConditionalOnProperty(
    name = "store.client.type",
    havingValue = "rest",
    matchIfMissing = true  // 기본값
)
class RestClientConfig {
    // StoreFeignClient 활성화
}

// 3. application.yml 설정
store:
  client:
    type: grpc  # 또는 rest (기본값)
```

## 단계적 마이그레이션 전략

### Phase 1: Dual Run (병렬 실행)
REST와 gRPC를 동시에 실행하여 검증

```kotlin
@Component
class StoreClientAdapter(
    @Qualifier("storeFeignClient")
    private val restClient: StoreClient,
    @Qualifier("storeGrpcClient")
    private val grpcClient: StoreClient
) {
    fun getStore(storeId: Long): StoreResponse? {
        return try {
            // 1. gRPC 호출
            val grpcResult = grpcClient.getStore(storeId)

            // 2. REST 호출 (비교용)
            val restResult = restClient.getStore(storeId)

            // 3. 결과 비교 및 로깅
            compareResults(grpcResult, restResult)

            // 4. gRPC 결과 반환
            grpcResult
        } catch (e: Exception) {
            // Fallback to REST
            logger.warn { "gRPC failed, falling back to REST" }
            restClient.getStore(storeId)
        }
    }
}
```

### Phase 2: Shadow Traffic
프로덕션 트래픽을 gRPC로 복제하여 테스트

```kotlin
@Component
class StoreClientAdapter(
    @Qualifier("storeFeignClient")
    private val primaryClient: StoreClient,  // 현재는 REST
    @Qualifier("storeGrpcClient")
    private val shadowClient: StoreClient    // Shadow gRPC
) {
    fun getStore(storeId: Long): StoreResponse? {
        // 1. 주 클라이언트 호출 (REST)
        val result = primaryClient.getStore(storeId)

        // 2. 백그라운드에서 gRPC 호출 (결과는 무시)
        CompletableFuture.runAsync {
            try {
                shadowClient.getStore(storeId)
            } catch (e: Exception) {
                logger.error(e) { "Shadow gRPC call failed" }
            }
        }

        return result
    }
}
```

### Phase 3: Full Migration
완전히 gRPC로 전환

```kotlin
@Component
class StoreClientAdapter(
    @Qualifier("storeGrpcClient")
    private val storeClient: StoreClient  // gRPC만 사용
) {
    // REST 코드 제거
}
```

## 의존성 추가 (gRPC 전환 시)

```kotlin
// build.gradle.kts
dependencies {
    // gRPC
    implementation("io.grpc:grpc-kotlin-stub:1.4.0")
    implementation("io.grpc:grpc-protobuf:1.58.0")
    implementation("io.grpc:grpc-netty-shaded:1.58.0")
    implementation("com.google.protobuf:protobuf-kotlin:3.24.0")

    // Feign (기존 유지 또는 제거)
    // implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
}
```

## 권장 전환 방법

**프로덕션 환경에서는 방법 3 (@Profile) 추천:**

1. **개발 환경**: REST 사용 (`spring.profiles.active=rest`)
2. **스테이징 환경**: gRPC 테스트 (`spring.profiles.active=grpc`)
3. **프로덕션 카나리**: 일부 트래픽만 gRPC (`spring.profiles.active=grpc`)
4. **프로덕션 전체**: 모두 gRPC로 전환

장점:
- Adapter 코드 수정 불필요
- 환경변수로 간단히 전환
- 롤백도 쉬움 (환경변수만 변경)

## 현재 의존성 주입 구조 확인

`StoreClientAdapterDependencyInjectionTest`를 실행하여 현재 구조를 검증할 수 있습니다.

```bash
./gradlew :customer-api:test --tests "*StoreClientAdapterDependencyInjectionTest"
```
