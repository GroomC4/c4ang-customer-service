package com.groom.customer.configuration.jpa

import jakarta.persistence.EntityManagerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.orm.jpa.HibernateProperties
import org.springframework.boot.autoconfigure.orm.jpa.HibernateSettings
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.orm.jpa.vendor.Database
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import javax.sql.DataSource

/**
 * JPA 설정
 *
 * Platform Core의 datasource-starter가 제공하는 Primary DataSource를 사용합니다.
 * - @Primary DataSource: DynamicRoutingDataSource (Master/Replica 자동 라우팅)
 * - @Transactional(readOnly = true): Replica DB 사용
 * - @Transactional(readOnly = false) 또는 기본: Master DB 사용
 */
@Configuration
@EnableJpaRepositories(
    basePackages = ["com.groom.customer"],
    entityManagerFactoryRef = "entityManagerFactory",
    transactionManagerRef = "transactionManager",
)
@EnableTransactionManagement
class JpaConfig {
    @Primary
    @Bean("entityManagerFactory")
    fun entityManagerFactory(
        dataSource: DataSource, // Platform Core가 제공하는 Primary DataSource (DynamicRoutingDataSource)
        jpaProperties: JpaProperties,
        hibernateProperties: HibernateProperties,
    ): LocalContainerEntityManagerFactoryBean =
        LocalContainerEntityManagerFactoryBean().apply {
            this.dataSource = dataSource
            this.setPackagesToScan("com.groom.customer")
            this.jpaVendorAdapter =
                HibernateJpaVendorAdapter().apply {
                    setDatabase(Database.POSTGRESQL)
                    setGenerateDdl(false)
                }
            // application.yml의 모든 JPA/Hibernate 설정을 자동으로 적용
            val vendorProperties =
                hibernateProperties.determineHibernateProperties(
                    jpaProperties.properties,
                    HibernateSettings(),
                )
            this.setJpaPropertyMap(
                buildMap {
                    putAll(vendorProperties)
                    // 추가 속성이 필요하면 여기에 추가
                    put("hibernate.jdbc.lob.non_contextual_creation", true)
                },
            )
        }

    @Bean("transactionManager")
    @Primary
    fun transactionManager(
        @Qualifier("entityManagerFactory") entityManagerFactory: EntityManagerFactory,
    ): PlatformTransactionManager = JpaTransactionManager(entityManagerFactory)
}
