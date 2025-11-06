package com.groom.customer.domain.model

import com.groom.customer.common.annotation.UnitTest
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

@UnitTest
class AddressTest :
    FunSpec({

        test("정상적인 주소로 Address 객체를 생성할 수 있다") {
            // given
            val validAddress = "서울시 강남구 테헤란로 123"

            // when
            val address = Address.from(validAddress)

            // then
            address.value shouldBe validAddress
        }

        test("주소 앞뒤 공백이 제거된다") {
            // given
            val addressWithSpaces = "  서울시 강남구 테헤란로 123  "

            // when
            val address = Address.from(addressWithSpaces)

            // then
            address.value shouldBe "서울시 강남구 테헤란로 123"
        }

        test("빈 주소는 예외가 발생한다") {
            // given
            val emptyAddress = ""

            // when & then
            shouldThrow<IllegalArgumentException> {
                Address.from(emptyAddress)
            }.message shouldContain "기본 주소는 비어 있을 수 없습니다."
        }

        test("공백만 있는 주소는 예외가 발생한다") {
            // given
            val blankAddress = "   "

            // when & then
            shouldThrow<IllegalArgumentException> {
                Address.from(blankAddress)
            }.message shouldContain "기본 주소는 비어 있을 수 없습니다."
        }

        test("다양한 형식의 주소를 허용한다") {
            // given
            val addresses =
                listOf(
                    "서울특별시 강남구 테헤란로 123",
                    "경기도 성남시 분당구 판교로 456",
                    "부산광역시 해운대구 우동 789",
                    "제주특별자치도 제주시 첨단로 101동 202호",
                )

            // when & then
            addresses.forEach { addressString ->
                val address = Address.from(addressString)
                address.value shouldBe addressString
            }
        }
    })
