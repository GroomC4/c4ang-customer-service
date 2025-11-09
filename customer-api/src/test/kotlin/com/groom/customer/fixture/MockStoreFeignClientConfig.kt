package com.groom.customer.fixture

import com.groom.customer.outbound.client.ProductListResponse
import com.groom.customer.outbound.client.StoreClient
import com.groom.customer.outbound.client.StoreListResponse
import com.groom.customer.outbound.client.StoreResponse
import com.groom.customer.outbound.client.StoreStatus
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import java.util.UUID

/**
 * 테스트용 MockStoreFeignClient 설정
 *
 * FeignClient를 mock하여 실제 Store 서비스 호출 없이 테스트 가능하도록 합니다.
 */
@TestConfiguration
@Profile("test", "k8s-test")
class MockStoreFeignClientConfig {
    @Bean
    @Primary
    fun mockStoreClient(): StoreClient =
        object : StoreClient {
            override fun create(
                ownerUserId: UUID,
                name: String,
                description: String?,
            ): StoreResponse =
                StoreResponse(
                    id = UUID.randomUUID(),
                    name = name,
                    description = description,
                    address = null,
                    phoneNumber = null,
                    ownerId = ownerUserId.mostSignificantBits, // UUID를 Long으로 변환 (테스트용)
                    status = StoreStatus.ACTIVE,
                )

            override fun getStore(storeId: UUID): StoreResponse =
                StoreResponse(
                    id = storeId,
                    name = "Mock Store",
                    description = "Mock Store Description",
                    address = null,
                    phoneNumber = null,
                    ownerId = 1L,
                    status = StoreStatus.ACTIVE,
                )

            override fun getStores(
                page: Int,
                size: Int,
            ): StoreListResponse =
                StoreListResponse(
                    content = emptyList(),
                    page = page,
                    size = size,
                    totalElements = 0L,
                    totalPages = 0,
                )

            override fun getStoreProducts(
                storeId: UUID,
                page: Int,
                size: Int,
            ): ProductListResponse =
                ProductListResponse(
                    content = emptyList(),
                    page = page,
                    size = size,
                    totalElements = 0L,
                    totalPages = 0,
                )
        }
}
