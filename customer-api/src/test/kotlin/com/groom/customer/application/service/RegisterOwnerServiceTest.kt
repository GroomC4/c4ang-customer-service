package com.groom.customer.application.service

import com.groom.customer.application.dto.RegisterOwnerCommand
import com.groom.customer.common.annotation.UnitTest
import com.groom.customer.common.enums.UserRole
import com.groom.customer.common.exception.UserException
import com.groom.customer.domain.model.User
import com.groom.customer.domain.service.NewStore
import com.groom.customer.domain.service.StorePolicy
import com.groom.customer.domain.service.UserFactory
import com.groom.customer.domain.service.UserPolicy
import com.groom.customer.fixture.NoOpsStoreAdapter
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
class RegisterOwnerServiceTest :
    BehaviorSpec({

        isolationMode = IsolationMode.InstancePerLeaf

        Given("유효한 판매자 회원가입 정보가 주어졌을 때") {
            val userRepository = mockk<UserRepositoryImpl>()
            val userPolicy = mockk<UserPolicy>()
            val userFactory = mockk<UserFactory>()
            val storePolicy = mockk<StorePolicy>()
            val storeFactory = NoOpsStoreAdapter()

            val registerOwnerService =
                RegisterOwnerService(
                    userRepository = userRepository,
                    userPolicy = userPolicy,
                    userFactory = userFactory,
                    storePolicy = storePolicy,
                    storeFactory = storeFactory,
                )

            val command =
                RegisterOwnerCommand(
                    username = "판매자김",
                    email = "owner@example.com",
                    rawPassword = "password123!",
                    phoneNumber = "010-9999-9999",
                    storeName = "테크 스토어",
                    storeDescription = "최신 전자제품 판매",
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
            every { storePolicy.checkStoreAlreadyExists(testUserId) } just runs

            When("판매자 회원가입을 진행하면") {
                val result = registerOwnerService.register(command)

                Then("회원가입이 성공하고 사용자와 스토어 정보를 반환한다") {
                    result shouldNotBe null
                    result.userId shouldBe testUserId.toString()
                    result.username shouldBe command.username
                    result.email shouldBe command.email
                    result.storeName shouldBe command.storeName
                    result.createdAt shouldNotBe null
                }

                Then("이메일 중복 확인, 사용자 생성, 스토어 중복 확인, 스토어 생성이 각각 한 번씩 호출된다") {
                    verify(exactly = 1) { userPolicy.checkAlreadyRegister(command.email, UserRole.OWNER) }
                    verify(exactly = 1) { userFactory.createNewOwner(any(), any(), any(), any()) }
                    verify(exactly = 1) { userRepository.save(createdUser) }
                    verify(exactly = 1) { storePolicy.checkStoreAlreadyExists(testUserId) }
                }
            }
        }

        Given("이미 가입한 이메일로 판매자 회원가입을 시도할 때") {
            val userRepository = mockk<UserRepositoryImpl>()
            val userPolicy = mockk<UserPolicy>()
            val userFactory = mockk<UserFactory>()
            val storePolicy = mockk<StorePolicy>()
            val storeFactory = NoOpsStoreAdapter()

            val registerOwnerService =
                RegisterOwnerService(
                    userRepository = userRepository,
                    userPolicy = userPolicy,
                    userFactory = userFactory,
                    storePolicy = storePolicy,
                    storeFactory = storeFactory,
                )

            val command =
                RegisterOwnerCommand(
                    username = "중복판매자",
                    email = "existing@example.com",
                    rawPassword = "password123!",
                    phoneNumber = "010-8888-8888",
                    storeName = "중복 스토어",
                    storeDescription = null,
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
                    verify(exactly = 0) { storePolicy.checkStoreAlreadyExists(any()) }
                }
            }
        }

        Given("이미 스토어를 보유한 사용자가 판매자 회원가입을 시도할 때") {
            val userRepository = mockk<UserRepositoryImpl>()
            val userPolicy = mockk<UserPolicy>()
            val userFactory = mockk<UserFactory>()
            val storePolicy = mockk<StorePolicy>()
            val storeFactory = NoOpsStoreAdapter()

            val registerOwnerService =
                RegisterOwnerService(
                    userRepository = userRepository,
                    userPolicy = userPolicy,
                    userFactory = userFactory,
                    storePolicy = storePolicy,
                    storeFactory = storeFactory,
                )

            val command =
                RegisterOwnerCommand(
                    username = "기존판매자",
                    email = "newowner@example.com",
                    rawPassword = "password123!",
                    phoneNumber = "010-7777-7777",
                    storeName = "신규 스토어",
                    storeDescription = "새로운 스토어",
                )

            every { userPolicy.checkAlreadyRegister(command.email, UserRole.OWNER) } just runs

            val testExternalId = UUID.randomUUID()
            val createdUser =
                mockk<User>(relaxed = true) {
                    every { id } returns testExternalId
                }

            every {
                userFactory.createNewOwner(any(), any(), any(), any())
            } returns createdUser

            val savedUser =
                mockk<User>(relaxed = true) {
                    every { id } returns testExternalId
                }

            every { userRepository.save(createdUser) } returns savedUser

            every { storePolicy.checkStoreAlreadyExists(testExternalId) } throws
                IllegalStateException("이미 스토어를 보유하고 있습니다.")

            When("회원가입을 진행하면") {
                Then("IllegalStateException 예외가 발생하고 스토어는 생성되지 않는다") {
                    shouldThrow<IllegalStateException> {
                        registerOwnerService.register(command)
                    }

                    verify(exactly = 1) { userPolicy.checkAlreadyRegister(command.email, UserRole.OWNER) }
                    verify(exactly = 1) { userFactory.createNewOwner(any(), any(), any(), any()) }
                    verify(exactly = 1) { userRepository.save(any()) }
                    verify(exactly = 1) { storePolicy.checkStoreAlreadyExists(testExternalId) }
                }
            }
        }
    })
