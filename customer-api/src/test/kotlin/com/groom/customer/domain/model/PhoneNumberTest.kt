package com.groom.customer.domain.model

import com.groom.customer.common.annotation.UnitTest
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

@UnitTest
class PhoneNumberTest :
    FunSpec({

        test("정상적인 전화번호 형식으로 PhoneNumber 객체를 생성할 수 있다") {
            // given
            val validPhoneNumber = "010-1234-5678"

            // when
            val phoneNumber = PhoneNumber.from(validPhoneNumber)

            // then
            phoneNumber.value shouldBe validPhoneNumber
        }

        test("010으로 시작하는 전화번호를 허용한다") {
            // given
            val phoneNumber = "010-1234-5678"

            // when
            val result = PhoneNumber.from(phoneNumber)

            // then
            result.value shouldBe phoneNumber
        }

        test("011, 016, 017, 018, 019로 시작하는 전화번호를 허용한다") {
            // given
            val phoneNumbers =
                listOf(
                    "011-123-5678",
                    "016-1234-5678",
                    "017-123-5678",
                    "018-1234-5678",
                    "019-123-5678",
                )

            // when & then
            phoneNumbers.forEach { phone ->
                val result = PhoneNumber.from(phone)
                result.value shouldBe phone
            }
        }

        test("빈 전화번호는 예외가 발생한다") {
            // given
            val blankPhoneNumber = "   "

            // when & then
            shouldThrow<IllegalArgumentException> {
                PhoneNumber.from(blankPhoneNumber)
            }.message shouldContain "전화번호는 비어 있을 수 없습니다."
        }

        test("하이픈이 없는 전화번호는 예외가 발생한다") {
            // given
            val phoneNumberWithoutHyphen = "01012345678"

            // when & then
            shouldThrow<IllegalArgumentException> {
                PhoneNumber.from(phoneNumberWithoutHyphen)
            }.message shouldContain "전화번호는 010-1234-5678 형식이어야 합니다."
        }

        test("잘못된 형식의 전화번호는 예외가 발생한다 - 잘못된 시작 번호") {
            // given
            val invalidPhoneNumber = "020-1234-5678"

            // when & then
            shouldThrow<IllegalArgumentException> {
                PhoneNumber.from(invalidPhoneNumber)
            }.message shouldContain "전화번호는 010-1234-5678 형식이어야 합니다."
        }

        test("잘못된 형식의 전화번호는 예외가 발생한다 - 잘못된 중간 자리수") {
            // given
            val invalidPhoneNumber = "010-12-5678"

            // when & then
            shouldThrow<IllegalArgumentException> {
                PhoneNumber.from(invalidPhoneNumber)
            }.message shouldContain "전화번호는 010-1234-5678 형식이어야 합니다."
        }

        test("잘못된 형식의 전화번호는 예외가 발생한다 - 잘못된 끝 자리수") {
            // given
            val invalidPhoneNumber = "010-1234-567"

            // when & then
            shouldThrow<IllegalArgumentException> {
                PhoneNumber.from(invalidPhoneNumber)
            }.message shouldContain "전화번호는 010-1234-5678 형식이어야 합니다."
        }
    })
