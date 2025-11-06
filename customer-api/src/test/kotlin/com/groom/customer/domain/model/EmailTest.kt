package com.groom.customer.domain.model

import com.groom.customer.common.annotation.UnitTest
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

@UnitTest
class EmailTest :
    FunSpec({

        test("정상적인 이메일 형식으로 Email 객체를 생성할 수 있다") {
            // given
            val validEmail = "test@example.com"

            // when
            val email = Email.from(validEmail)

            // then
            email.value shouldBe validEmail
        }

        test("이메일은 소문자로 정규화된다") {
            // given
            val upperCaseEmail = "TEST@EXAMPLE.COM"

            // when
            val email = Email.from(upperCaseEmail)

            // then
            email.value shouldBe "test@example.com"
        }

        test("이메일 앞뒤 공백이 제거된다") {
            // given
            val emailWithSpaces = "  test@example.com  "

            // when
            val email = Email.from(emailWithSpaces)

            // then
            email.value shouldBe "test@example.com"
        }

        test("빈 이메일은 예외가 발생한다") {
            // given
            val blankEmail = "   "

            // when & then
            shouldThrow<IllegalArgumentException> {
                Email.from(blankEmail)
            }.message shouldContain "이메일은 비어 있을 수 없습니다."
        }

        test("잘못된 이메일 형식은 예외가 발생한다 - @ 누락") {
            // given
            val invalidEmail = "testexample.com"

            // when & then
            shouldThrow<IllegalArgumentException> {
                Email.from(invalidEmail)
            }.message shouldContain "이메일 형식이 올바르지 않습니다."
        }

        test("잘못된 이메일 형식은 예외가 발생한다 - 도메인 누락") {
            // given
            val invalidEmail = "test@"

            // when & then
            shouldThrow<IllegalArgumentException> {
                Email.from(invalidEmail)
            }.message shouldContain "이메일 형식이 올바르지 않습니다."
        }

        test("잘못된 이메일 형식은 예외가 발생한다 - TLD 누락") {
            // given
            val invalidEmail = "test@example"

            // when & then
            shouldThrow<IllegalArgumentException> {
                Email.from(invalidEmail)
            }.message shouldContain "이메일 형식이 올바르지 않습니다."
        }

        test("다양한 유효한 이메일 형식을 허용한다") {
            // given
            val validEmails =
                listOf(
                    "user@example.com",
                    "user.name@example.com",
                    "user+tag@example.co.kr",
                    "user_name@example-domain.com",
                    "123@example.com",
                )

            // when & then
            validEmails.forEach { emailString ->
                val email = Email.from(emailString)
                email.value shouldBe emailString.trim().lowercase()
            }
        }
    })
