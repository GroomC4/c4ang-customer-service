package com.groom.customer.adapter.outbound.security

import com.groom.customer.configuration.jwt.JwtProperties
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("JwtProperties 보안 테스트")
class JwtPropertiesTest {

    companion object {
        // 테스트용 RSA 키 (문자열 연결로 작성)
        private const val TEST_PRIVATE_KEY = "-----BEGIN PRIVATE KEY-----\n" +
            "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCvD5SDEkoxLqiL" +
            "gTeNbjLe2vpkgDEGtGh1lYPB/AALcEEC23v6EhMiTx41DDIw9X6oD+G/7GwbKpa9" +
            "sxhvjYtGBUuMPmUhXwR9No13mRXeeiacbnsFRMUzp9iP9z0/vJTCGINcRrZA56B6" +
            "buCbDq/d0H4YdE+Pz7+cMIsMGBrpg3YO2m1XDQaDWuxfXLv0WfVmvReAv+gyRT7K" +
            "NzzvMdGeqD7eiMWYOkRqfvNY+Sox5EdlTB4/JTOWVO1JV8f2JcFWcb8vxH3i/+cZ" +
            "H11tOi7TWdM5Glz242nf3pEWKfjytzf1+/OgkXFOm9a8i8UigCS9jzeRo0xFYK32" +
            "+KYvhfHRAgMBAAECggEAQ1cg79KHS6gBGbjZH8R2ORfPHf3Z3hRj4mdjSamgcsX5" +
            "nBnF9Qoi5h29JvbMD90/nXKOin9tjn2xgsNz8OVn38WFrCsMR+v/FBN6E7mFmhEu" +
            "7RnqpLoxiY9VVPvsSapHJuq7DTH+RbVUHASuzba2nALpnoqPWGi38mMR+dMD9zMB" +
            "t8N0KEz4CvA8bfOKrh1xGskFPNRf2ScOJ2JmqkoBGyXrAobJGwp27Au7UNEdR496" +
            "uRglbOrurr/eK9A1LvulVShgpxHX4FVPWJK3Zjdgc7tkqhBUpQh8uv0hlEL8Sv+1" +
            "aluV5u958x9G86ioNAiblBoZnYRRcPB1Y37txwmWgQKBgQDZakjqZEFtuORxHFmK" +
            "6LK2L9SdgCu/st85Ms0b35bI7MOMeYr2U5M4+e3hkxL3C9tFpZqtQIGYYU2l0OnC" +
            "oLw1ctESZgGM/KiAaUydKvamszT1JtT2vtLGlpnYg7RjV7+sIaQUkH283n8BO+Ro" +
            "vjqJG6XZbGrczgJ/01gHDM01CQKBgQDOIQhnT1BBHbqBDEyaiTmf7PCkBiCM2eiJ" +
            "fx5vfSXQcktRkze8XKRs1qKVJiQfR3TtmJa1fMwl3NbkkR9FawNYqSO2kAROr9Gl" +
            "i6wAPooPO5Vl2GES5V+5nI8G/iy1aPcYXq5Dz/yf1o+kbO/ytwERUXglTaMHnI4w" +
            "NhCtDEQQiQKBgGKfzR3OhsOgKLiKtK/HqTHt9pPPzYizOoF24wYu4faZOIejpv7g" +
            "oJsq/Nbj4amBjmFEoyrOZTtbgF6kqzWntljEkcS30yJChqlhmuh80dCC4JYInHil" +
            "zXVaYcWO0ShzaLZLuGO/u9oOUCyeH5nIGUOS8CP2A2/QX9/eXkMscnYJAoGAEqRM" +
            "JUO4B1uP7XHWT7ePXZZJIRxovzRJ4n17nCueSt67TxJYXRGn0SwMIh8D70xAF+jP" +
            "4HP75oS1bpBtWpLWB6OsVitqKE+gTy91i8QcKkqCNWa/SL0zzg6JpOFB29o1Vp/h" +
            "dMKPn0kBTqaHgNTqJM3QZtdBokOXXGbXVT8hvLkCgYARs07jQ+hdwz4kz2P3X++C" +
            "CkD3HEvJYbx5smrwvYu6DQgOvOdl4wFYKmmd35/S2u7Two8xKZysEZs9Ts6FxPLG" +
            "H59lezuu8Uh/uKx34qGueJie9iXkwtcadD1H3lSpyck+hvzJkbNH4peDWhtthW+P" +
            "eTiMG2mOPQRt+kY6QDPz8w==\n" +
            "-----END PRIVATE KEY-----"

        private const val TEST_PUBLIC_KEY = "-----BEGIN PUBLIC KEY-----\n" +
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEArw+UgxJKMS6oi4E3jW4y" +
            "3tr6ZIAxBrRodZWDwfwAC3BBAtt7+hITIk8eNQwyMPV+qA/hv+xsGyqWvbMYb42L" +
            "RgVLjD5lIV8EfTaNd5kV3nomnG57BUTFM6fYj/c9P7yUwhiDXEa2QOegem7gmw6v" +
            "3dB+GHRPj8+/nDCLDBga6YN2DtptVw0Gg1rsX1y79Fn1Zr0XgL/oMkU+yjc87zHR" +
            "nqg+3ojFmDpEan7zWPkqMeRHZUwePyUzllTtSVfH9iXBVnG/L8R94v/nGR9dbTou" +
            "01nTORpc9uNp396RFin48rc39fvzoJFxTpvWvIvFIoAkvY83kaNMRWCt9vimL4Xx" +
            "0QIDAQAB\n" +
            "-----END PUBLIC KEY-----"
    }

    @Nested
    @DisplayName("RSA 키 검증 테스트")
    inner class RsaKeyValidationTest {
        @Test
        @DisplayName("유효한 RSA 키 쌍은 허용된다")
        fun `should accept valid RSA key pair`() {
            // when
            val properties = JwtProperties(
                privateKey = TEST_PRIVATE_KEY,
                publicKey = TEST_PUBLIC_KEY,
                issuer = "test-issuer",
            )

            // then
            assertThat(properties.privateKey).contains("PRIVATE KEY")
            assertThat(properties.publicKey).contains("PUBLIC KEY")
        }

        @Test
        @DisplayName("빈 private key는 거부된다")
        fun `should reject empty private key`() {
            // when & then
            assertThatThrownBy {
                JwtProperties(
                    privateKey = "",
                    publicKey = TEST_PUBLIC_KEY,
                    issuer = "test-issuer",
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("JWT privateKey must not be blank")
        }

        @Test
        @DisplayName("빈 public key는 거부된다")
        fun `should reject empty public key`() {
            // when & then
            assertThatThrownBy {
                JwtProperties(
                    privateKey = TEST_PRIVATE_KEY,
                    publicKey = "",
                    issuer = "test-issuer",
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("JWT publicKey must not be blank")
        }

        @Test
        @DisplayName("PEM 형식이 아닌 private key는 거부된다")
        fun `should reject non-PEM private key`() {
            // when & then
            assertThatThrownBy {
                JwtProperties(
                    privateKey = "not-a-valid-pem-key",
                    publicKey = TEST_PUBLIC_KEY,
                    issuer = "test-issuer",
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("JWT privateKey must be in PEM format")
        }

        @Test
        @DisplayName("PEM 형식이 아닌 public key는 거부된다")
        fun `should reject non-PEM public key`() {
            // when & then
            assertThatThrownBy {
                JwtProperties(
                    privateKey = TEST_PRIVATE_KEY,
                    publicKey = "not-a-valid-pem-key",
                    issuer = "test-issuer",
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("JWT publicKey must be in PEM format")
        }
    }

    @Nested
    @DisplayName("만료 시간 검증 테스트")
    inner class ExpirationTimeValidationTest {
        @Test
        @DisplayName("양수의 액세스 토큰 만료 시간은 허용된다")
        fun `should accept positive access token expiration time`() {
            // when
            val properties = JwtProperties(
                privateKey = TEST_PRIVATE_KEY,
                publicKey = TEST_PUBLIC_KEY,
                issuer = "test-issuer",
                accessTokenExpirationMinutes = 30,
            )

            // then
            assertThat(properties.accessTokenExpirationMinutes).isEqualTo(30)
        }

        @Test
        @DisplayName("0 또는 음수의 액세스 토큰 만료 시간은 거부된다")
        fun `should reject zero or negative access token expiration time`() {
            // when & then: 0
            assertThatThrownBy {
                JwtProperties(
                    privateKey = TEST_PRIVATE_KEY,
                    publicKey = TEST_PUBLIC_KEY,
                    issuer = "test-issuer",
                    accessTokenExpirationMinutes = 0,
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("JWT accessTokenExpirationMinutes must be positive")

            // when & then: 음수
            assertThatThrownBy {
                JwtProperties(
                    privateKey = TEST_PRIVATE_KEY,
                    publicKey = TEST_PUBLIC_KEY,
                    issuer = "test-issuer",
                    accessTokenExpirationMinutes = -1,
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("JWT accessTokenExpirationMinutes must be positive")
        }

        @Test
        @DisplayName("양수의 리프레시 토큰 만료 시간은 허용된다")
        fun `should accept positive refresh token expiration time`() {
            // when
            val properties = JwtProperties(
                privateKey = TEST_PRIVATE_KEY,
                publicKey = TEST_PUBLIC_KEY,
                issuer = "test-issuer",
                refreshTokenExpirationDays = 7,
            )

            // then
            assertThat(properties.refreshTokenExpirationDays).isEqualTo(7)
        }

        @Test
        @DisplayName("0 또는 음수의 리프레시 토큰 만료 시간은 거부된다")
        fun `should reject zero or negative refresh token expiration time`() {
            // when & then: 0
            assertThatThrownBy {
                JwtProperties(
                    privateKey = TEST_PRIVATE_KEY,
                    publicKey = TEST_PUBLIC_KEY,
                    issuer = "test-issuer",
                    refreshTokenExpirationDays = 0,
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("JWT refreshTokenExpirationDays must be positive")

            // when & then: 음수
            assertThatThrownBy {
                JwtProperties(
                    privateKey = TEST_PRIVATE_KEY,
                    publicKey = TEST_PUBLIC_KEY,
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
            // when: issuer를 명시하지 않음
            val properties = JwtProperties(
                privateKey = TEST_PRIVATE_KEY,
                publicKey = TEST_PUBLIC_KEY,
            )

            // then
            assertThat(properties.issuer).isEqualTo("ecommerce-service-api")
        }

        @Test
        @DisplayName("기본 keyId 값이 올바르게 설정된다")
        fun `should use default key id value`() {
            // when: keyId를 명시하지 않음
            val properties = JwtProperties(
                privateKey = TEST_PRIVATE_KEY,
                publicKey = TEST_PUBLIC_KEY,
            )

            // then
            assertThat(properties.keyId).isEqualTo("ecommerce-key-1")
        }

        @Test
        @DisplayName("기본 액세스 토큰 만료 시간이 올바르게 설정된다")
        fun `should use default access token expiration time`() {
            // when: accessTokenExpirationMinutes를 명시하지 않음
            val properties = JwtProperties(
                privateKey = TEST_PRIVATE_KEY,
                publicKey = TEST_PUBLIC_KEY,
            )

            // then
            assertThat(properties.accessTokenExpirationMinutes).isEqualTo(5)
        }

        @Test
        @DisplayName("기본 리프레시 토큰 만료 시간이 올바르게 설정된다")
        fun `should use default refresh token expiration time`() {
            // when: refreshTokenExpirationDays를 명시하지 않음
            val properties = JwtProperties(
                privateKey = TEST_PRIVATE_KEY,
                publicKey = TEST_PUBLIC_KEY,
            )

            // then
            assertThat(properties.refreshTokenExpirationDays).isEqualTo(3)
        }
    }
}
