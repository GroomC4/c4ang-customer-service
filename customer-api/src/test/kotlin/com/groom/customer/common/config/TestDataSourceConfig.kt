package com.groom.customer.common.config

import com.groom.customer.configuration.jpa.DataSourceType
import com.groom.customer.configuration.jpa.DynamicRoutingDataSource
import com.groom.platform.testSupport.BaseContainerExtension
import com.zaxxer.hikari.HikariDataSource
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy
import javax.sql.DataSource

/**
 * 통합 테스트를 위한 데이터 소스 설정.
 *
 * BaseContainerExtension에서 실행된 PostgreSQL Primary/Replica와 Redis에 동적으로 연결합니다.
 */
@Profile("test")
@Configuration
class TestDataSourceConfig {
    @Bean
    fun masterDataSource(): HikariDataSource =
        DataSourceBuilder
            .create()
            .type(HikariDataSource::class.java)
            .url(BaseContainerExtension.getPrimaryJdbcUrl())
            .driverClassName("org.postgresql.Driver")
            .username("test")
            .password("test")
            .build()

    @Bean
    fun replicaDataSource(): HikariDataSource =
        DataSourceBuilder
            .create()
            .type(HikariDataSource::class.java)
            .url(BaseContainerExtension.getReplicaJdbcUrl())
            .driverClassName("org.postgresql.Driver")
            .username("test")
            .password("test")
            .build()

    @Bean
    fun routingDataSource(
        @Qualifier("masterDataSource") masterDataSource: DataSource,
        @Qualifier("replicaDataSource") replicaDataSource: DataSource,
    ): DataSource {
        val dynamicRoutingDataSource = DynamicRoutingDataSource()
        val targetDataSources: Map<Any, Any> =
            mapOf(
                DataSourceType.MASTER to masterDataSource,
                DataSourceType.REPLICA to replicaDataSource,
            )
        dynamicRoutingDataSource.setTargetDataSources(targetDataSources)
        dynamicRoutingDataSource.setDefaultTargetDataSource(masterDataSource)

        return dynamicRoutingDataSource
    }

    @Primary
    @Bean
    fun dataSource(
        @Qualifier("routingDataSource") dataSource: DataSource,
    ): DataSource = LazyConnectionDataSourceProxy(dataSource)

    @Bean
    fun redisConnectionFactory(): RedisConnectionFactory =
        LettuceConnectionFactory(
            BaseContainerExtension.getRedisHost(),
            BaseContainerExtension.getRedisPort(),
        )
}
