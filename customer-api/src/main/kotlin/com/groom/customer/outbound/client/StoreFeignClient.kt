package com.groom.customer.outbound.client

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import java.util.UUID

/**
 * Store 서비스와 통신하기 위한 Feign Client
 */
@FeignClient(
    name = "store-service",
    url = "\${feign.clients.store-service.url:http://localhost:8081}",
)
interface StoreFeignClient : StoreClient {
    /**
     * 상점 생성
     *
     * @param ownerUserId 상점 소유자 사용자 ID
     * @param name 상점 이름
     * @param description 상점 설명
     * @return 상점 정보
     */
    @PostMapping("/api/v1/stores")
    override fun create(
        ownerUserId: UUID,
        name: String,
        description: String?,
    ): StoreResponse

    /**
     * 특정 상점 정보 조회
     *
     * @param storeId 상점 ID
     * @return 상점 정보
     */
    @GetMapping("/api/v1/stores/{storeId}")
    override fun getStore(
        @PathVariable storeId: UUID,
    ): StoreResponse

    /**
     * 상점 목록 조회
     *
     * @param page 페이지 번호 (기본값: 0)
     * @param size 페이지 크기 (기본값: 20)
     * @return 상점 목록
     */
    @GetMapping("/api/v1/stores")
    override fun getStores(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): StoreListResponse

    /**
     * 특정 상점의 상품 목록 조회
     *
     * @param storeId 상점 ID
     * @param page 페이지 번호 (기본값: 0)
     * @param size 페이지 크기 (기본값: 20)
     * @return 상품 목록
     */
    @GetMapping("/api/v1/stores/{storeId}/products")
    override fun getStoreProducts(
        @PathVariable storeId: UUID,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ProductListResponse
}

/**
 * 상점 정보 응답 DTO
 */
data class StoreResponse(
    val id: UUID,
    val name: String,
    val description: String?,
    val address: String?,
    val phoneNumber: String?,
    val ownerId: Long,
    val status: StoreStatus,
)

/**
 * 상점 목록 응답 DTO
 */
data class StoreListResponse(
    val content: List<StoreResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

/**
 * 상품 목록 응답 DTO
 */
data class ProductListResponse(
    val content: List<ProductResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

/**
 * 상품 정보 응답 DTO
 */
data class ProductResponse(
    val id: UUID,
    val storeId: Long,
    val name: String,
    val description: String?,
    val price: Long,
    val stockQuantity: Int,
    val status: ProductStatus,
)

/**
 * 상점 상태
 */
enum class StoreStatus {
    ACTIVE,
    INACTIVE,
    SUSPENDED,
}

/**
 * 상품 상태
 */
enum class ProductStatus {
    AVAILABLE,
    OUT_OF_STOCK,
    DISCONTINUED,
}
