package com.groom.customer.application.service

import com.groom.customer.application.dto.RegisterCustomerCommand
import com.groom.customer.common.annotation.UnitTest
import com.groom.customer.common.enums.UserRole
import com.groom.customer.common.exception.UserException
import com.groom.customer.domain.model.User
import com.groom.customer.domain.service.UserFactory
import com.groom.customer.domain.service.UserPolicy
import com.groom.customer.outbound.repository.UserRepositoryImpl
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import java.util.UUID

@UnitTest
class RegisterCustomerServiceTest :
    BehaviorSpec({

        isolationMode = IsolationMode.InstancePerLeaf

        Given("유효한 회원가입 정보가 주어졌을 때") {
            val userRepository = mockk<UserRepositoryImpl>()
            val userPolicy = mockk<UserPolicy>()
            val userFactory = mockk<UserFactory>()
            val registerCustomerService =
                RegisterCustomerService(
                    userRepository = userRepository,
                    userPolicy = userPolicy,
                    userFactory = userFactory,
                )
            val command =
                RegisterCustomerCommand(
                    username = "홍길동",
                    email = "hong@example.com",
                    rawPassword = "password123!",
                    defaultAddress = "서울시 강남구 테헤란로 123",
                    defaultPhoneNumber = "010-1234-5678",
                )

            every { userPolicy.checkAlreadyRegister(command.email, UserRole.CUSTOMER) } just runs

            val createdUser =
                mockk<User>(relaxed = true) {
                    every { username } returns command.username
                    every { email } returns command.email
                    every { role } returns UserRole.CUSTOMER
                    every { isActive } returns true
                }

            every {
                userFactory.createNewCustomer(
                    username = any(),
                    email = any(),
                    passwordHash = command.rawPassword,
                    defaultAddress = any(),
                    defaultPhoneNumber = any(),
                )
            } returns createdUser

            val testUserId = UUID.randomUUID()
            val savedUser =
                mockk<User>(relaxed = true) {
                    every { id } returns testUserId
                    every { username } returns command.username
                    every { email } returns command.email
                    every { role } returns UserRole.CUSTOMER
                    every { isActive } returns true
                    every { createdAt } returns java.time.LocalDateTime.now()
                }

            every { userRepository.save(createdUser) } returns savedUser

            When("회원가입을 진행하면") {
                val result = registerCustomerService.register(command)

                Then("회원가입이 성공하고 결과를 반환한다") {
                    result shouldNotBe null
                    result.userId shouldBe testUserId.toString()
                    result.username shouldBe command.username
                    result.email shouldBe command.email
                    result.role shouldBe UserRole.CUSTOMER
                    result.isActive shouldBe true
                    result.createdAt shouldNotBe null
                }

                Then("중복 확인, 팩토리 생성, 저장이 각각 한 번씩 호출된다") {
                    verify(exactly = 1) { userPolicy.checkAlreadyRegister(command.email, UserRole.CUSTOMER) }
                    verify(exactly = 1) { userFactory.createNewCustomer(any(), any(), any(), any(), any()) }
                    verify(exactly = 1) { userRepository.save(createdUser) }
                }
            }
        }

        Given("이미 가입한 이메일로 회원가입을 시도할 때") {
            val userRepository = mockk<UserRepositoryImpl>()
            val userPolicy = mockk<UserPolicy>()
            val userFactory = mockk<UserFactory>()
            val registerCustomerService =
                RegisterCustomerService(
                    userRepository = userRepository,
                    userPolicy = userPolicy,
                    userFactory = userFactory,
                )

            val command =
                RegisterCustomerCommand(
                    username = "홍길동",
                    email = "existing@example.com",
                    rawPassword = "password123!",
                    defaultAddress = "서울시 강남구 테헤란로 123",
                    defaultPhoneNumber = "010-1234-5678",
                )

            every { userPolicy.checkAlreadyRegister(command.email, UserRole.CUSTOMER) } throws
                UserException.DuplicateEmail(email = command.email)

            When("회원가입을 진행하면") {
                Then("UserException.DuplicateEmail 예외가 발생하고 중복 확인만 호출된다") {
                    val exception =
                        shouldThrow<UserException.DuplicateEmail> {
                            registerCustomerService.register(command)
                        }
                    exception.email shouldBe command.email

                    verify(exactly = 1) { userPolicy.checkAlreadyRegister(command.email, UserRole.CUSTOMER) }
                    verify(exactly = 0) { userFactory.createNewCustomer(any(), any(), any(), any(), any()) }
                    verify(exactly = 0) { userRepository.save(any()) }
                }
            }
        }

        Given("여러 명의 사용자가 동시에 회원가입을 진행할 때") {
            val userRepository = mockk<UserRepositoryImpl>()
            val userPolicy = mockk<UserPolicy>()
            val userFactory = mockk<UserFactory>()
            val registerCustomerService =
                RegisterCustomerService(
                    userRepository = userRepository,
                    userPolicy = userPolicy,
                    userFactory = userFactory,
                )

            val commands =
                listOf(
                    RegisterCustomerCommand(
                        username = "김철수",
                        email = "kim@example.com",
                        rawPassword = "password1",
                        defaultAddress = "서울시",
                        defaultPhoneNumber = "010-1111-1111",
                    ),
                    RegisterCustomerCommand(
                        username = "이영희",
                        email = "lee@example.com",
                        rawPassword = "password2",
                        defaultAddress = "경기도",
                        defaultPhoneNumber = "010-2222-2222",
                    ),
                )

            commands.forEach { command ->
                every { userPolicy.checkAlreadyRegister(command.email, UserRole.CUSTOMER) } just runs

                val createdUser =
                    mockk<User>(relaxed = true) {
                        every { username } returns command.username
                        every { email } returns command.email
                    }

                every {
                    userFactory.createNewCustomer(
                        username = match { it.value == command.username },
                        email = match { it.value == command.email },
                        passwordHash = command.rawPassword,
                        defaultAddress = any(),
                        defaultPhoneNumber = any(),
                    )
                } returns createdUser

                val savedUser =
                    mockk<User>(relaxed = true) {
                        every { id } returns UUID.randomUUID()
                        every { username } returns command.username
                        every { email } returns command.email
                        every { role } returns UserRole.CUSTOMER
                        every { isActive } returns true
                        every { createdAt } returns java.time.LocalDateTime.now()
                    }

                every { userRepository.save(createdUser) } returns savedUser
            }

            When("각각 회원가입을 진행하면") {
                val results = commands.map { registerCustomerService.register(it) }

                Then("모든 사용자의 회원가입이 성공한다") {
                    results.size shouldBe 2
                    results[0].username shouldBe "김철수"
                    results[0].email shouldBe "kim@example.com"
                    results[1].username shouldBe "이영희"
                    results[1].email shouldBe "lee@example.com"
                }

                Then("각 사용자에 대해 중복 확인, 팩토리 생성, 저장이 호출된다") {
                    commands.forEach { command ->
                        verify(exactly = 1) { userPolicy.checkAlreadyRegister(command.email, UserRole.CUSTOMER) }
                    }
                    verify(exactly = 2) { userFactory.createNewCustomer(any(), any(), any(), any(), any()) }
                    verify(exactly = 2) { userRepository.save(any()) }
                }
            }
        }
    })
