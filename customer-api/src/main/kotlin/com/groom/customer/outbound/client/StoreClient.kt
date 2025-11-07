package com.groom.customer.outbound.client

import java.util.UUID

/**
 * Store 서비스와 통신하기 위한 추상 인터페이스
 *
 * 구현체:
 * - StoreFeignClient: REST API 통신 (현재)
 * - StoreGrpcClient: gRPC 통신 (향후 추가 가능)
 *
 * 이 인터페이스를 통해 통신 방식을 추상화하여,
 * 향후 REST에서 gRPC로 변경 시 Adapter 코드 수정 없이 전환 가능합니다.
 */
interface StoreClient {
    /**
     * 특정 상점 정보 조회
     */
    fun create(
        ownerUserId: UUID,
        name: String,
        description: String?,
    ): StoreResponse

    /**
     * 특정 상점 정보 조회
     */
    fun getStore(storeId: UUID): StoreResponse

    /**
     * 상점 목록 조회
     */
    fun getStores(
        page: Int,
        size: Int,
    ): StoreListResponse

    /**
     * 특정 상점의 상품 목록 조회
     */
    fun getStoreProducts(
        storeId: UUID,
        page: Int,
        size: Int,
    ): ProductListResponse
}
