package com.groom.customer.domain.model

import com.groom.customer.common.annotation.UnitTest
import com.groom.customer.common.enums.UserRole
import com.groom.customer.domain.service.UserFactory
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.util.UUID

@UnitTest
class UserTest :
    ShouldSpec({

        val userFactory = UserFactory()

        context("UserFactory를 통한 Customer 생성") {
            should("새로운 고객 사용자를 생성한다") {
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

                // then
                user.username shouldBe username.value
                user.email shouldBe email.value
                user.passwordHash shouldBe passwordHash
                user.role shouldBe UserRole.CUSTOMER
                user.isActive shouldBe true
            }

            should("UserProfile과 양방향 관계가 설정된다") {
                // given
                val username = Username.from("김철수")
                val email = Email.from("kim@example.com")
                val passwordHash = "hashed-password"
                val defaultAddress = Address.from("서울시")
                val defaultPhoneNumber = PhoneNumber.from("010-1111-1111")

                // when
                val user =
                    userFactory.createNewCustomer(
                        username = username,
                        email = email,
                        passwordHash = passwordHash,
                        defaultAddress = defaultAddress,
                        defaultPhoneNumber = defaultPhoneNumber,
                    )

                // then
                user.profile.shouldNotBeNull()
                user.profile!!.user shouldBe user
                user.profile!!.fullName shouldBe username.value
                user.profile!!.phoneNumber shouldBe defaultPhoneNumber.value
                user.profile!!.contactEmail shouldBe email.value
                user.profile!!.defaultAddress shouldBe defaultAddress.value
            }

            should("동일한 정보로 생성해도 매번 새로운 인스턴스가 생성된다") {
                // given
                val username = Username.from("이영희")
                val email = Email.from("lee@example.com")
                val passwordHash = "hashed-password"
                val defaultAddress = Address.from("경기도")
                val defaultPhoneNumber = PhoneNumber.from("010-2222-2222")

                // when
                val user1 =
                    userFactory.createNewCustomer(
                        username = username,
                        email = email,
                        passwordHash = passwordHash,
                        defaultAddress = defaultAddress,
                        defaultPhoneNumber = defaultPhoneNumber,
                    )
                val user2 =
                    userFactory.createNewCustomer(
                        username = username,
                        email = email,
                        passwordHash = passwordHash,
                        defaultAddress = defaultAddress,
                        defaultPhoneNumber = defaultPhoneNumber,
                    )

                // then
                user1 shouldNotBe user2
                user1.profile shouldNotBe user2.profile
            }
        }

        context("User 엔티티의 기본 속성") {
            should("생성 시 활성화 상태이다") {
                // given & when
                val user =
                    userFactory.createNewCustomer(
                        username = Username.from("박민수"),
                        email = Email.from("park@example.com"),
                        passwordHash = "hashed-password",
                        defaultAddress = Address.from("부산시"),
                        defaultPhoneNumber = PhoneNumber.from("010-3333-3333"),
                    )

                // then
                user.isActive shouldBe true
            }

            should("생성 시 lastLoginAt과 deletedAt은 null이다") {
                // given & when
                val user =
                    userFactory.createNewCustomer(
                        username = Username.from("최수진"),
                        email = Email.from("choi@example.com"),
                        passwordHash = "hashed-password",
                        defaultAddress = Address.from("대전시"),
                        defaultPhoneNumber = PhoneNumber.from("010-4444-4444"),
                    )

                // then
                user.lastLoginAt shouldBe null
                user.deletedAt shouldBe null
            }
        }

        context("equals와 hashCode") {
            should("id가 같으면 동일한 객체로 판단한다") {
                // given
                val sameExternalId = UUID.randomUUID()
                val user1 =
                    userFactory
                        .createNewCustomer(
                            username = Username.from("강호동"),
                            email = Email.from("kang@example.com"),
                            passwordHash = "hashed-password",
                            defaultAddress = Address.from("서울시"),
                            defaultPhoneNumber = PhoneNumber.from("010-6666-6666"),
                        ).apply {
                            id = sameExternalId
                        }

                val user2 =
                    userFactory
                        .createNewCustomer(
                            username = Username.from("유재석"),
                            email = Email.from("yoo@example.com"),
                            passwordHash = "different-password",
                            defaultAddress = Address.from("경기도"),
                            defaultPhoneNumber = PhoneNumber.from("010-7777-7777"),
                        ).apply {
                            id = sameExternalId
                        }

                // when & then
                user1 shouldBe user2
                user1.hashCode() shouldBe user2.hashCode()
            }

            should("id가 다르면 다른 객체로 판단한다") {
                // given
                val user1 =
                    userFactory
                        .createNewCustomer(
                            username = Username.from("김구라"),
                            email = Email.from("kim@example.com"),
                            passwordHash = "hashed-password",
                            defaultAddress = Address.from("서울시"),
                            defaultPhoneNumber = PhoneNumber.from("010-8888-8888"),
                        ).apply {
                            id = UUID.randomUUID()
                        }

                val user2 =
                    userFactory
                        .createNewCustomer(
                            username = Username.from("김구라"),
                            email = Email.from("kim@example.com"),
                            passwordHash = "hashed-password",
                            defaultAddress = Address.from("서울시"),
                            defaultPhoneNumber = PhoneNumber.from("010-8888-8888"),
                        ).apply {
                            id = UUID.randomUUID()
                        }

                // when & then
                user1 shouldNotBe user2
            }
        }
    })
