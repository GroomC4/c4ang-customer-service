package com.groom.customer

import org.junit.jupiter.api.Test
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

class GenerateBcryptHashTest {
    @Test
    fun generateBcryptHash() {
        val encoder = BCryptPasswordEncoder()
        val password = "password123!"

        // Generate multiple hashes to show they're different each time
        repeat(3) {
            val hash = encoder.encode(password)
            println("Hash $it: $hash")
            println("Matches: ${encoder.matches(password, hash)}")
        }

        // Test the hash that was in the SQL
        val oldHash = "\$2a\$10\$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"
        println("\nOld hash matches: ${encoder.matches(password, oldHash)}")
    }
}
