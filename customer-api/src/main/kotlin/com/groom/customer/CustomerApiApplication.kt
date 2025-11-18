package com.groom.customer

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.retry.annotation.EnableRetry
import org.springframework.scheduling.annotation.EnableAsync

@SpringBootApplication(
    exclude = [
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration::class,
    ]
)
@ConfigurationPropertiesScan
@EnableAsync
@EnableRetry
@EnableFeignClients
class CustomerApiApplication

fun main(args: Array<String>) {
    runApplication<CustomerApiApplication>(*args)
}
