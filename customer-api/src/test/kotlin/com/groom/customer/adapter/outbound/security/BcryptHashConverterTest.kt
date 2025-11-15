package com.groom.customer.adapter.outbound.security

import com.groom.customer.common.annotation.UnitTest
import com.groom.customer.configuration.encryption.EncryptHashEncoderRegistry
import com.groom.customer.adapter.outbound.security.BcryptHashConverter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldMatch
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

@UnitTest
class BcryptHashConverterTest :
    FunSpec({

        val converter = BcryptHashConverter()

        // PasswordEncoder 초기화
        beforeSpec {
            EncryptHashEncoderRegistry.setPasswordEncoder(BCryptPasswordEncoder())
        }

        test("평문 비밀번호를 bcrypt 해시로 변환한다") {
            // given
            val plainPassword = "mySecurePassword123!"

            // when
            val hashedPassword = converter.convertToDatabaseColumn(plainPassword)

            // then
            hashedPassword shouldNotBe null
            hashedPassword shouldNotBe plainPassword
            hashedPassword!! shouldMatch """^\$2[aby]\$\d{2}\$[./A-Za-z0-9]{53}$"""
        }

        test("이미 bcrypt 해시된 값은 다시 인코딩하지 않는다") {
            // given
            val alreadyHashed = "\$2a\$10\$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"

            // when
            val result = converter.convertToDatabaseColumn(alreadyHashed)

            // then
            result shouldBe alreadyHashed
        }

        test("null 값은 null로 반환한다") {
            // when
            val result = converter.convertToDatabaseColumn(null)

            // then
            result.shouldBeNull()
        }

        test("데이터베이스에서 읽은 값은 그대로 반환한다") {
            // given
            val dbData = "\$2a\$10\$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"

            // when
            val result = converter.convertToEntityAttribute(dbData)

            // then
            result shouldBe dbData
        }

        test("데이터베이스에서 null을 읽으면 null을 반환한다") {
            // when
            val result = converter.convertToEntityAttribute(null)

            // then
            result.shouldBeNull()
        }

        test("다양한 bcrypt 알고리즘 버전을 인식한다") {
            // given
            val bcryptVersions =
                listOf(
                    "\$2a\$10\$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy", // $2a$
                    "\$2b\$10\$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy", // $2b$
                    "\$2y\$10\$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy", // $2y$
                )

            // when & then
            bcryptVersions.forEach { hash ->
                val result = converter.convertToDatabaseColumn(hash)
                result shouldBe hash
            }
        }

        test("같은 평문 비밀번호라도 매번 다른 해시가 생성된다") {
            // given
            val password = "samePassword123"

            // when
            val hash1 = converter.convertToDatabaseColumn(password)
            val hash2 = converter.convertToDatabaseColumn(password)

            // then
            hash1 shouldNotBe hash2
            hash1!! shouldMatch """^\$2[aby]\$\d{2}\$[./A-Za-z0-9]{53}$"""
            hash2!! shouldMatch """^\$2[aby]\$\d{2}\$[./A-Za-z0-9]{53}$"""
        }

        test("빈 문자열도 bcrypt 해시로 변환된다") {
            // given
            val emptyPassword = ""

            // when
            val hashedPassword = converter.convertToDatabaseColumn(emptyPassword)

            // then
            hashedPassword shouldNotBe null
            hashedPassword!! shouldMatch """^\$2[aby]\$\d{2}\$[./A-Za-z0-9]{53}$"""
        }
    })
