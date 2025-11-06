package com.groom.customer.domain.model

import com.groom.customer.common.annotation.UnitTest
import com.groom.customer.domain.service.UserFactory
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.util.UUID

@UnitTest
class UserProfileTest :
    ShouldSpec({

        val userFactory = UserFactory()

        context("UserFactory를 통한 UserProfile 생성") {
            should("User와 함께 생성되고 필수 정보가 올바르게 저장된다") {
                // given
                val username = Username.from("홍길동")
                val email = Email.from("hong@example.com")
                val passwordHash = "hashed-password"
                val defaultAddress = Address.from("서울시 강남구 테헤란로 123")
                val defaultPhoneNumber = PhoneNumber.from("010-1234-5678")

                // when
                val user =
                    userFactory.createNewCustomer(
                        username = username,
                        email = email,
                        passwordHash = passwordHash,
                        defaultAddress = defaultAddress,
                        defaultPhoneNumber = defaultPhoneNumber,
                    )
                val profile = user.profile!!

                // then
                profile.fullName shouldBe username.value
                profile.phoneNumber shouldBe defaultPhoneNumber.value
                profile.contactEmail shouldBe email.value
                profile.defaultAddress shouldBe defaultAddress.value
                profile.user shouldBe user
            }

            should("User와 양방향 관계가 설정된다") {
                // given
                val username = Username.from("김철수")
                val email = Email.from("kim@example.com")
                val defaultAddress = Address.from("서울시")
                val defaultPhoneNumber = PhoneNumber.from("010-1111-1111")

                // when
                val user =
                    userFactory.createNewCustomer(
                        username = username,
                        email = email,
                        passwordHash = "hashed-password",
                        defaultAddress = defaultAddress,
                        defaultPhoneNumber = defaultPhoneNumber,
                    )
                val profile = user.profile!!

                // then
                profile.user shouldBe user
                user.profile shouldBe profile
            }

            should("fullName은 User의 username과 동일하다") {
                // given
                val username = Username.from("이영희")
                val email = Email.from("lee@example.com")
                val defaultAddress = Address.from("경기도")
                val defaultPhoneNumber = PhoneNumber.from("010-2222-2222")

                // when
                val user =
                    userFactory.createNewCustomer(
                        username = username,
                        email = email,
                        passwordHash = "hashed-password",
                        defaultAddress = defaultAddress,
                        defaultPhoneNumber = defaultPhoneNumber,
                    )
                val profile = user.profile!!

                // then
                profile.fullName shouldBe user.username
            }
        }

        context("UserProfile 엔티티의 기본 속성") {
            should("생성 시 필수 정보가 올바르게 설정된다") {
                // given
                val username = Username.from("최수진")
                val email = Email.from("choi@example.com")
                val defaultAddress = Address.from("대전시")
                val defaultPhoneNumber = PhoneNumber.from("010-4444-4444")

                // when
                val user =
                    userFactory.createNewCustomer(
                        username = username,
                        email = email,
                        passwordHash = "hashed-password",
                        defaultAddress = defaultAddress,
                        defaultPhoneNumber = defaultPhoneNumber,
                    )
                val profile = user.profile!!

                // then
                profile.fullName shouldBe username.value
                profile.phoneNumber shouldBe defaultPhoneNumber.value
                profile.contactEmail shouldBe email.value
                profile.defaultAddress shouldBe defaultAddress.value
            }
        }

        context("equals와 hashCode") {
            should("id가 같으면 동일한 객체로 판단한다") {
                // given
                val user1 =
                    userFactory.createNewCustomer(
                        username = Username.from("강호동"),
                        email = Email.from("kang@example.com"),
                        passwordHash = "hashed-password",
                        defaultAddress = Address.from("서울시"),
                        defaultPhoneNumber = PhoneNumber.from("010-5555-5555"),
                    )
                val user2 =
                    userFactory.createNewCustomer(
                        username = Username.from("유재석"),
                        email = Email.from("yoo@example.com"),
                        passwordHash = "hashed-password",
                        defaultAddress = Address.from("경기도"),
                        defaultPhoneNumber = PhoneNumber.from("010-5555-5555"),
                    )

                val profile1 = user1.profile!!
                val profile2 = user2.profile!!

                // DB에 저장되었다고 가정하고 같은 UUID를 수동으로 설정
                val sameId = UUID.randomUUID()
                profile1.id = sameId
                profile2.id = sameId

                // when & then
                profile1 shouldBe profile2
                profile1.hashCode() shouldBe profile2.hashCode()
            }

            should("id가 다르면 다른 객체로 판단한다") {
                // given
                val user1 =
                    userFactory.createNewCustomer(
                        username = Username.from("김구라"),
                        email = Email.from("kim1@example.com"),
                        passwordHash = "hashed-password",
                        defaultAddress = Address.from("서울시"),
                        defaultPhoneNumber = PhoneNumber.from("010-6666-6666"),
                    )
                val user2 =
                    userFactory.createNewCustomer(
                        username = Username.from("김구라"),
                        email = Email.from("kim2@example.com"),
                        passwordHash = "hashed-password",
                        defaultAddress = Address.from("서울시"),
                        defaultPhoneNumber = PhoneNumber.from("010-6666-6666"),
                    )

                val profile1 = user1.profile!!
                val profile2 = user2.profile!!

                // DB에 저장되었다고 가정하고 다른 UUID를 수동으로 설정
                profile1.id = UUID.randomUUID()
                profile2.id = UUID.randomUUID()

                // when & then
                profile1 shouldNotBe profile2
            }
        }

        context("프로필 정보 검증") {
            should("모든 필수 정보가 올바르게 저장된다") {
                // given
                val username = Username.from("정다은")
                val email = Email.from("jung@example.com")
                val defaultAddress = Address.from("광주시 서구 치평동 123-45")
                val defaultPhoneNumber = PhoneNumber.from("010-7777-7777")

                // when
                val user =
                    userFactory.createNewCustomer(
                        username = username,
                        email = email,
                        passwordHash = "hashed-password",
                        defaultAddress = defaultAddress,
                        defaultPhoneNumber = defaultPhoneNumber,
                    )
                val profile = user.profile!!

                // then
                profile.fullName shouldBe "정다은"
                profile.phoneNumber shouldBe "010-7777-7777"
                profile.contactEmail shouldBe "jung@example.com"
                profile.defaultAddress shouldBe "광주시 서구 치평동 123-45"
                profile.user shouldBe user
            }
        }
    })
