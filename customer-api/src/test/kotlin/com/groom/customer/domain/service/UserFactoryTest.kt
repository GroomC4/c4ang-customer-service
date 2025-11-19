package com.groom.customer.domain.service

import com.groom.customer.common.annotation.UnitTest
import com.groom.customer.domain.model.Address
import com.groom.customer.domain.model.Email
import com.groom.customer.domain.model.PhoneNumber
import com.groom.customer.domain.model.UserRole
import com.groom.customer.domain.model.Username
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

@UnitTest
class UserFactoryTest :
    ShouldSpec({

        val userFactory = UserFactory()

        context("createNewCustomer") {
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

            should("UserProfile이 함께 생성되고 양방향 관계가 설정된다") {
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

            should("UserProfile의 fullName은 User의 username과 동일하다") {
                // given
                val username = Username.from("이영희")
                val email = Email.from("lee@example.com")
                val passwordHash = "hashed-password"
                val defaultAddress = Address.from("경기도")
                val defaultPhoneNumber = PhoneNumber.from("010-2222-2222")

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
                user.profile!!.fullName shouldBe user.username
            }

            should("동일한 정보로 생성해도 매번 새로운 인스턴스가 생성된다") {
                // given
                val username = Username.from("박민수")
                val email = Email.from("park@example.com")
                val passwordHash = "hashed-password"
                val defaultAddress = Address.from("부산시")
                val defaultPhoneNumber = PhoneNumber.from("010-3333-3333")

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

            should("생성된 User는 활성화 상태이다") {
                // given
                val username = Username.from("최수진")
                val email = Email.from("choi@example.com")
                val passwordHash = "hashed-password"
                val defaultAddress = Address.from("대전시")
                val defaultPhoneNumber = PhoneNumber.from("010-4444-4444")

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
                user.isActive shouldBe true
            }

            should("생성된 User는 CUSTOMER 역할을 가진다") {
                // given
                val username = Username.from("정다은")
                val email = Email.from("jung@example.com")
                val passwordHash = "hashed-password"
                val defaultAddress = Address.from("광주시")
                val defaultPhoneNumber = PhoneNumber.from("010-5555-5555")

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
                user.role shouldBe UserRole.CUSTOMER
            }

            should("생성 시 lastLoginAt과 deletedAt은 null이다") {
                // given
                val username = Username.from("강호동")
                val email = Email.from("kang@example.com")
                val passwordHash = "hashed-password"
                val defaultAddress = Address.from("서울시")
                val defaultPhoneNumber = PhoneNumber.from("010-6666-6666")

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
                user.lastLoginAt shouldBe null
                user.deletedAt shouldBe null
            }

            should("다양한 주소 형식을 지원한다") {
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
                    val user =
                        userFactory.createNewCustomer(
                            username = Username.from("테스터"),
                            email = Email.from("test@example.com"),
                            passwordHash = "hashed-password",
                            defaultAddress = Address.from(addressString),
                            defaultPhoneNumber = PhoneNumber.from("010-1234-5678"),
                        )

                    user.profile!!.defaultAddress shouldBe addressString
                }
            }

            should("다양한 전화번호 형식을 지원한다") {
                // given
                val phoneNumbers =
                    listOf(
                        "010-1234-5678",
                        "011-123-5678",
                        "016-1234-5678",
                        "017-123-5678",
                        "018-1234-5678",
                        "019-123-5678",
                    )

                // when & then
                phoneNumbers.forEach { phone ->
                    val user =
                        userFactory.createNewCustomer(
                            username = Username.from("테스터"),
                            email = Email.from("test@example.com"),
                            passwordHash = "hashed-password",
                            defaultAddress = Address.from("서울시"),
                            defaultPhoneNumber = PhoneNumber.from(phone),
                        )

                    user.profile!!.phoneNumber shouldBe phone
                }
            }
        }
    })
