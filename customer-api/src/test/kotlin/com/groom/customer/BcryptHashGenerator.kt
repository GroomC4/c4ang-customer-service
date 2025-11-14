package com.groom.customer

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

fun main() {
    val encoder = BCryptPasswordEncoder()
    val password = "password123!"
    val hash = encoder.encode(password)

    println("Password: $password")
    println("BCrypt Hash: $hash")
    println("Verification: ${encoder.matches(password, hash)}")
}
