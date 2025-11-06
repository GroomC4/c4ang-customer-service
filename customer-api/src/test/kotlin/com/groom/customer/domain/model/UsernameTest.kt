package com.groom.customer.domain.model

import com.groom.customer.common.annotation.UnitTest
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

@UnitTest
class UsernameTest :
    FunSpec({

        test("정상적인 한글 이름으로 Username 객체를 생성할 수 있다") {
            // given
            val validName = "홍길동"

            // when
            val username = Username.from(validName)

            // then
            username.value shouldBe validName
        }

        test("2자에서 10자 사이의 한글 이름을 허용한다") {
            // given
            val validNames =
                listOf(
                    "김철수",
                    "이영희",
                    "박민수영희철",
                    "가나다라마바사아자",
                )

            // when & then
            validNames.forEach { name ->
                val username = Username.from(name)
                username.value shouldBe name
            }
        }

        test("이름 앞뒤 공백이 제거된다") {
            // given
            val nameWithSpaces = "  홍길동  "

            // when
            val username = Username.from(nameWithSpaces)

            // then
            username.value shouldBe "홍길동"
        }

        test("빈 이름은 예외가 발생한다") {
            // given
            val blankName = "   "

            // when & then
            shouldThrow<IllegalArgumentException> {
                Username.from(blankName)
            }.message shouldContain "사용자 이름은 비어 있을 수 없습니다."
        }

        test("2자 미만의 이름은 예외가 발생한다") {
            // given
            val shortName = "김"

            // when & then
            shouldThrow<IllegalArgumentException> {
                Username.from(shortName)
            }.message shouldContain "사용자 이름은 2자 이상 10자 이하의 한글로 입력해주세요."
        }

        test("10자 초과의 이름은 예외가 발생한다") {
            // given
            val longName = "가나다라마바사아자차카"

            // when & then
            shouldThrow<IllegalArgumentException> {
                Username.from(longName)
            }.message shouldContain "사용자 이름은 2자 이상 10자 이하의 한글로 입력해주세요."
        }

        test("영문이 포함된 이름은 예외가 발생한다") {
            // given
            val nameWithEnglish = "홍길동Kim"

            // when & then
            shouldThrow<IllegalArgumentException> {
                Username.from(nameWithEnglish)
            }.message shouldContain "사용자 이름은 한글만 사용할 수 있습니다."
        }

        test("숫자가 포함된 이름은 예외가 발생한다") {
            // given
            val nameWithNumber = "홍길동123"

            // when & then
            shouldThrow<IllegalArgumentException> {
                Username.from(nameWithNumber)
            }.message shouldContain "사용자 이름은 한글만 사용할 수 있습니다."
        }

        test("특수문자가 포함된 이름은 예외가 발생한다") {
            // given
            val nameWithSpecialChar = "홍길동!"

            // when & then
            shouldThrow<IllegalArgumentException> {
                Username.from(nameWithSpecialChar)
            }.message shouldContain "사용자 이름은 한글만 사용할 수 있습니다."
        }

        test("공백이 포함된 이름은 예외가 발생한다") {
            // given
            val nameWithSpace = "홍 길동"

            // when & then
            shouldThrow<IllegalArgumentException> {
                Username.from(nameWithSpace)
            }.message shouldContain "사용자 이름은 한글만 사용할 수 있습니다."
        }
    })
