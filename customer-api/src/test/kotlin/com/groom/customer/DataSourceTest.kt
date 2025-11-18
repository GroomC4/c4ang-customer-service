package com.groom.customer

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import javax.sql.DataSource

@SpringBootTest(properties = ["spring.profiles.active=test"])
class DataSourceTest {

    @Autowired(required = false)
    private val dataSource: DataSource? = null

    @Test
    fun `DataSource bean should exist`() {
        println("DataSource: $dataSource")
        println("DataSource class: ${dataSource?.javaClass}")
        assert(dataSource != null) { "DataSource bean should not be null" }
    }
}
