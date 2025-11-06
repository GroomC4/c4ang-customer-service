package com.groom.customer.security.jwt

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("JwtProperties 보안 테스트")
class JwtPropertiesTest {
    @Nested
    @DisplayName("비밀 키 강도 검증 테스트")
    inner class SecretKeyValidationTest {
        @Test
        @DisplayName("충분히 긴 비밀 키는 허용된다")
        fun `should accept secret key with sufficient length`() {
            // given: 32자 이상의 비밀 키
            val validSecret = "this-is-a-very-strong-secret-key-with-more-than-32-characters"

            // when
            val properties =
                JwtProperties(
                    secret = validSecret,
                    issuer = "test-issuer",
                )

            // then
            assertThat(properties.secret).isEqualTo(validSecret)
        }

        @Test
        @DisplayName("정확히 32자의 비밀 키는 허용된다")
        fun `should accept secret key with exactly 32 characters`() {
            // given: 정확히 32자의 비밀 키
            val validSecret = "12345678901234567890123456789012" // 32 characters

            // when
            val properties =
                JwtProperties(
                    secret = validSecret,
                    issuer = "test-issuer",
                )

            // then
            assertThat(properties.secret).isEqualTo(validSecret)
            assertThat(properties.secret.length).isEqualTo(32)
        }

        @Test
        @DisplayName("32자 미만의 약한 비밀 키는 거부된다")
        fun `should reject weak secret key with less than 32 characters`() {
            // given: 32자 미만의 약한 비밀 키
            val weakSecret = "short-secret" // 12 characters

            // when & then
            assertThatThrownBy {
                JwtProperties(
                    secret = weakSecret,
                    issuer = "test-issuer",
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("JWT secret must be at least 32 characters long")
                .hasMessageContaining("Current length: 12")
        }

        @Test
        @DisplayName("빈 문자열 비밀 키는 거부된다")
        fun `should reject empty secret key`() {
            // given
            val emptySecret = ""

            // when & then
            assertThatThrownBy {
                JwtProperties(
                    secret = emptySecret,
                    issuer = "test-issuer",
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("JWT secret must not be blank")
        }

        @Test
        @DisplayName("공백만 있는 비밀 키는 거부된다")
        fun `should reject blank secret key`() {
            // given
            val blankSecret = "   "

            // when & then
            assertThatThrownBy {
                JwtProperties(
                    secret = blankSecret,
                    issuer = "test-issuer",
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("JWT secret must not be blank")
        }

        @Test
        @DisplayName("예측 가능한 약한 비밀 키(예: password)는 검증을 통과하지 못한다")
        fun `should reject predictable weak secret key`() {
            // given: 예측 가능한 약한 비밀 키들 (32자 미만)
            val weakSecrets =
                listOf(
                    "password",
                    "12345678",
                    "secret",
                    "admin123",
                    "qwerty",
                )

            // when & then: 모두 길이 검증에서 실패
            weakSecrets.forEach { weakSecret ->
                assertThatThrownBy {
                    JwtProperties(
                        secret = weakSecret,
                        issuer = "test-issuer",
                    )
                }.isInstanceOf(IllegalArgumentException::class.java)
                    .hasMessageContaining("JWT secret must be at least 32 characters long")
            }
        }
    }

    @Nested
    @DisplayName("만료 시간 검증 테스트")
    inner class ExpirationTimeValidationTest {
        @Test
        @DisplayName("양수의 액세스 토큰 만료 시간은 허용된다")
        fun `should accept positive access token expiration time`() {
            // given
            val validSecret = "this-is-a-very-strong-secret-key-with-more-than-32-characters"

            // when
            val properties =
                JwtProperties(
                    secret = validSecret,
                    issuer = "test-issuer",
                    accessTokenExpirationMinutes = 30,
                )

            // then
            assertThat(properties.accessTokenExpirationMinutes).isEqualTo(30)
        }

        @Test
        @DisplayName("0 또는 음수의 액세스 토큰 만료 시간은 거부된다")
        fun `should reject zero or negative access token expiration time`() {
            // given
            val validSecret = "this-is-a-very-strong-secret-key-with-more-than-32-characters"

            // when & then: 0
            assertThatThrownBy {
                JwtProperties(
                    secret = validSecret,
                    issuer = "test-issuer",
                    accessTokenExpirationMinutes = 0,
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("JWT accessTokenExpirationMinutes must be positive")

            // when & then: 음수
            assertThatThrownBy {
                JwtProperties(
                    secret = validSecret,
                    issuer = "test-issuer",
                    accessTokenExpirationMinutes = -1,
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("JWT accessTokenExpirationMinutes must be positive")
        }

        @Test
        @DisplayName("양수의 리프레시 토큰 만료 시간은 허용된다")
        fun `should accept positive refresh token expiration time`() {
            // given
            val validSecret = "this-is-a-very-strong-secret-key-with-more-than-32-characters"

            // when
            val properties =
                JwtProperties(
                    secret = validSecret,
                    issuer = "test-issuer",
                    refreshTokenExpirationDays = 7,
                )

            // then
            assertThat(properties.refreshTokenExpirationDays).isEqualTo(7)
        }

        @Test
        @DisplayName("0 또는 음수의 리프레시 토큰 만료 시간은 거부된다")
        fun `should reject zero or negative refresh token expiration time`() {
            // given
            val validSecret = "this-is-a-very-strong-secret-key-with-more-than-32-characters"

            // when & then: 0
            assertThatThrownBy {
                JwtProperties(
                    secret = validSecret,
                    issuer = "test-issuer",
                    refreshTokenExpirationDays = 0,
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("JWT refreshTokenExpirationDays must be positive")

            // when & then: 음수
            assertThatThrownBy {
                JwtProperties(
                    secret = validSecret,
                    issuer = "test-issuer",
                    refreshTokenExpirationDays = -1,
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("JWT refreshTokenExpirationDays must be positive")
        }
    }

    @Nested
    @DisplayName("기본값 테스트")
    inner class DefaultValueTest {
        @Test
        @DisplayName("기본 Issuer 값이 올바르게 설정된다")
        fun `should use default issuer value`() {
            // given
            val validSecret = "this-is-a-very-strong-secret-key-with-more-than-32-characters"

            // when: issuer를 명시하지 않음
            val properties = JwtProperties(secret = validSecret)

            // then
            assertThat(properties.issuer).isEqualTo("ecommerce-service-api")
        }

        @Test
        @DisplayName("기본 액세스 토큰 만료 시간이 올바르게 설정된다")
        fun `should use default access token expiration time`() {
            // given
            val validSecret = "this-is-a-very-strong-secret-key-with-more-than-32-characters"

            // when: accessTokenExpirationMinutes를 명시하지 않음
            val properties = JwtProperties(secret = validSecret)

            // then
            assertThat(properties.accessTokenExpirationMinutes).isEqualTo(5)
        }

        @Test
        @DisplayName("기본 리프레시 토큰 만료 시간이 올바르게 설정된다")
        fun `should use default refresh token expiration time`() {
            // given
            val validSecret = "this-is-a-very-strong-secret-key-with-more-than-32-characters"

            // when: refreshTokenExpirationDays를 명시하지 않음
            val properties = JwtProperties(secret = validSecret)

            // then
            assertThat(properties.refreshTokenExpirationDays).isEqualTo(3)
        }
    }
}
