package com.groom.customer.configuration

import com.zaxxer.hikari.HikariDataSource
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

/**
 * DataSource 설정
 *
 * Platform Core의 datasource-starter와 함께 사용됩니다.
 * - masterDataSource: Primary DB (Write 작업)
 * - replicaDataSource: Replica DB (Read 작업)
 *
 * Platform Core의 DataSourceAutoConfiguration이 이 Bean들을 찾아서
 * DynamicRoutingDataSource로 래핑하고, @Transactional(readOnly) 기반으로
 * 자동 라우팅을 수행합니다.
 */
@Configuration
class DataSourceConfig {
    /**
     * Master DataSource Properties
     */
    @Bean
    @ConfigurationProperties("spring.datasource.master")
    fun masterDataSourceProperties(): DataSourceProperties = DataSourceProperties()

    /**
     * Master DataSource Bean
     *
     * @Transactional(readOnly = false) 또는 @Transactional 없는 경우 사용됩니다.
     */
    @Bean
    fun masterDataSource(): DataSource =
        masterDataSourceProperties()
            .initializeDataSourceBuilder()
            .type(HikariDataSource::class.java)
            .build()

    /**
     * Replica DataSource Properties
     */
    @Bean
    @ConfigurationProperties("spring.datasource.replica")
    fun replicaDataSourceProperties(): DataSourceProperties = DataSourceProperties()

    /**
     * Replica DataSource Bean
     *
     * @Transactional(readOnly = true)인 경우 사용됩니다.
     */
    @Bean
    fun replicaDataSource(): DataSource =
        replicaDataSourceProperties()
            .initializeDataSourceBuilder()
            .type(HikariDataSource::class.java)
            .build()
}
