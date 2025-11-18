package com.groom.customer.configuration.jackson

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.avro.AvroMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder

/**
 * Jackson Avro Configuration
 *
 * Avro SpecificRecord 객체를 JSON으로 직렬화하기 위한 설정
 * - contract-hub의 UserInternalResponse 등을 REST API에서 반환할 때 사용
 */
@Configuration
class JacksonAvroConfig {
    @Bean
    @Primary
    fun objectMapper(builder: Jackson2ObjectMapperBuilder): ObjectMapper =
        builder.build<ObjectMapper>().apply {
            // Avro SpecificRecord를 JSON으로 직렬화할 수 있도록 설정
            // Avro 객체의 Schema 정보를 무시하고 일반 POJO처럼 처리
            registerModule(com.fasterxml.jackson.module.kotlin.KotlinModule.Builder().build())
        }
}
