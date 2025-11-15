package com.groom.customer.fixture

import com.groom.customer.adapter.outbound.client.ProductListResponse
import com.groom.customer.adapter.outbound.client.ProductResponse
import com.groom.customer.adapter.outbound.client.ProductStatus
import com.groom.customer.adapter.outbound.client.StoreClient
import com.groom.customer.adapter.outbound.client.StoreListResponse
import com.groom.customer.adapter.outbound.client.StoreResponse
import com.groom.customer.adapter.outbound.client.StoreStatus
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.util.UUID

private val logger = KotlinLogging.logger {}

/**
 * 테스트 환경 전용 StoreClient 구현체
 *
 * 실제 Store 서비스와 통신하지 않고 모킹된 응답을 반환합니다.
 * 통합 테스트에서 외부 의존성 없이 Customer 서비스의 비즈니스 로직을 검증할 수 있습니다.
 *
 * @Profile("test") - 테스트 환경에서만 활성화
 * @Primary - StoreFeignClient보다 우선 순위 높음
 */
@Component
@Profile("test")
@Primary
class MockStoreClient : StoreClient {
    companion object {
        // 테스트에서 사용할 Mock 데이터
        private val mockStores = mutableMapOf<UUID, StoreResponse>()

        /**
         * 테스트 간 격리를 위해 Mock 데이터 초기화
         */
        fun reset() {
            mockStores.clear()
        }
    }

    override fun create(
        ownerUserId: UUID,
        name: String,
        description: String?,
    ): StoreResponse {
        logger.info { "[MockStoreClient] create() called - name: $name, ownerUserId: $ownerUserId" }

        val storeId = UUID.randomUUID()
        val store =
            StoreResponse(
                id = storeId,
                name = name,
                description = description,
                address = "Mock Address 123",
                phoneNumber = "010-1234-5678",
                ownerId = ownerUserId.hashCode().toLong(), // UUID를 Long으로 변환 (Mock)
                status = StoreStatus.ACTIVE,
            )

        mockStores[storeId] = store
        logger.debug { "[MockStoreClient] Store created: $store" }

        return store
    }

    override fun getStore(storeId: UUID): StoreResponse {
        logger.info { "[MockStoreClient] getStore() called - storeId: $storeId" }

        return mockStores[storeId] ?: run {
            // 존재하지 않는 경우 기본 Mock 응답 반환
            StoreResponse(
                id = storeId,
                name = "Mock Store",
                description = "Mock Description",
                address = "Mock Address 123",
                phoneNumber = "010-1234-5678",
                ownerId = 1L,
                status = StoreStatus.ACTIVE,
            )
        }
    }

    override fun getStores(
        page: Int,
        size: Int,
    ): StoreListResponse {
        logger.info { "[MockStoreClient] getStores() called - page: $page, size: $size" }

        val stores = mockStores.values.toList()
        val totalElements = stores.size.toLong()
        val totalPages = (totalElements + size - 1) / size

        val content =
            stores
                .drop(page * size)
                .take(size)

        return StoreListResponse(
            content = content,
            page = page,
            size = size,
            totalElements = totalElements,
            totalPages = totalPages.toInt(),
        )
    }

    override fun getStoreProducts(
        storeId: UUID,
        page: Int,
        size: Int,
    ): ProductListResponse {
        logger.info { "[MockStoreClient] getStoreProducts() called - storeId: $storeId, page: $page, size: $size" }

        // Mock 상품 목록 생성
        val products =
            listOf(
                ProductResponse(
                    id = UUID.randomUUID(),
                    storeId = storeId.hashCode().toLong(),
                    name = "Mock Product 1",
                    description = "Mock Product Description 1",
                    price = 10000L,
                    stockQuantity = 100,
                    status = ProductStatus.AVAILABLE,
                ),
                ProductResponse(
                    id = UUID.randomUUID(),
                    storeId = storeId.hashCode().toLong(),
                    name = "Mock Product 2",
                    description = "Mock Product Description 2",
                    price = 20000L,
                    stockQuantity = 50,
                    status = ProductStatus.AVAILABLE,
                ),
            )

        val totalElements = products.size.toLong()
        val totalPages = (totalElements + size - 1) / size

        val content =
            products
                .drop(page * size)
                .take(size)

        return ProductListResponse(
            content = content,
            page = page,
            size = size,
            totalElements = totalElements,
            totalPages = totalPages.toInt(),
        )
    }
}
