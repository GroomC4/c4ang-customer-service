package com.groom.customer.common

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

/**
 * 통합 테스트를 위한 Base 클래스
 *
 * platform-core testcontainers-starter를 사용하여 통합 테스트 환경 구성
 * - PostgreSQL (Primary + Replica)
 * - Redis
 *
 * 모든 통합 테스트는 이 클래스를 상속받아 사용
 *
 * @see <a href="https://github.com/GroomC4/c4ang-platform-core">platform-core testcontainers-starter</a>
 */
@SpringBootTest(
    properties = [
        // PostgreSQL
        "testcontainers.postgres.enabled=true",
        "testcontainers.postgres.replica-enabled=true",
        "testcontainers.postgres.schema-location=project:sql/schema.sql",
        "testcontainers.postgres.database=testdb",
        "testcontainers.postgres.username=test",
        "testcontainers.postgres.password=test",

        // Redis
        "testcontainers.redis.enabled=true",
    ],
)
@ActiveProfiles("test")
abstract class IntegrationTestBase
