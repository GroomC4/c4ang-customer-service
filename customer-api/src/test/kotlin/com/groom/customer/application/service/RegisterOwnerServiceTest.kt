package com.groom.customer.application.service

import com.groom.customer.application.dto.RegisterOwnerCommand
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
import kotlin.jvm.java

@UnitTest
class RegisterOwnerServiceTest :
    BehaviorSpec({

        isolationMode = IsolationMode.InstancePerLeaf

        Given("유효한 판매자 회원가입 정보가 주어졌을 때") {
            val userRepository = mockk<UserRepositoryImpl>()
            val userPolicy = mockk<UserPolicy>()
            val userFactory = mockk<UserFactory>()

            val registerOwnerService =
                RegisterOwnerService(
                    userRepository = userRepository,
                    userPolicy = userPolicy,
                    userFactory = userFactory,
                )

            val command =
                RegisterOwnerCommand(
                    username = "판매자김",
                    email = "owner@example.com",
                    rawPassword = "password123!",
                    phoneNumber = "010-9999-9999",
                )

            every { userPolicy.checkAlreadyRegister(command.email, UserRole.OWNER) } just runs

            val testUserId = UUID.randomUUID()
            val createdUser =
                mockk<User>(relaxed = true) {
                    every { username } returns command.username
                    every { email } returns command.email
                    every { role } returns UserRole.OWNER
                    every { isActive } returns true
                    every { id } returns testUserId
                }

            every {
                userFactory.createNewOwner(
                    username = any(),
                    email = any(),
                    passwordHash = command.rawPassword,
                    phoneNumber = any(),
                )
            } returns createdUser

            val savedUser =
                mockk<User>(relaxed = true) {
                    every { id } returns testUserId
                    every { username } returns command.username
                    every { email } returns command.email
                    every { role } returns UserRole.OWNER
                    every { isActive } returns true
                    every { createdAt } returns java.time.LocalDateTime.now()
                }

            every { userRepository.save(createdUser) } returns savedUser

            When("판매자 회원가입을 진행하면") {
                val result = registerOwnerService.register(command)

                Then("회원가입이 성공하고 사용자 정보를 반환한다") {
                    result shouldNotBe null
                    result.userId shouldBe testUserId.toString()
                    result.username shouldBe command.username
                    result.email shouldBe command.email
                    result.createdAt shouldNotBe null
                }

                Then("이메일 중복 확인, 사용자 생성이 각각 한 번씩 호출된다") {
                    verify(exactly = 1) { userPolicy.checkAlreadyRegister(command.email, UserRole.OWNER) }
                    verify(exactly = 1) { userFactory.createNewOwner(any(), any(), any(), any()) }
                    verify(exactly = 1) { userRepository.save(createdUser) }
                }
            }
        }

        Given("이미 가입한 이메일로 판매자 회원가입을 시도할 때") {
            val userRepository = mockk<UserRepositoryImpl>()
            val userPolicy = mockk<UserPolicy>()
            val userFactory = mockk<UserFactory>()

            val registerOwnerService =
                RegisterOwnerService(
                    userRepository = userRepository,
                    userPolicy = userPolicy,
                    userFactory = userFactory,
                )

            val command =
                RegisterOwnerCommand(
                    username = "중복판매자",
                    email = "existing@example.com",
                    rawPassword = "password123!",
                    phoneNumber = "010-8888-8888",
                )

            every { userPolicy.checkAlreadyRegister(command.email, UserRole.OWNER) } throws
                UserException.DuplicateEmail(email = command.email)

            When("회원가입을 진행하면") {
                Then("UserException.DuplicateEmail 예외가 발생하고 이메일 중복 확인만 호출된다") {
                    val exception =
                        shouldThrow<UserException.DuplicateEmail> {
                            registerOwnerService.register(command)
                        }
                    exception.email shouldBe command.email

                    verify(exactly = 1) { userPolicy.checkAlreadyRegister(command.email, UserRole.OWNER) }
                    verify(exactly = 0) { userFactory.createNewOwner(any(), any(), any(), any()) }
                    verify(exactly = 0) { userRepository.save(any()) }
                }
            }
        }
    })
