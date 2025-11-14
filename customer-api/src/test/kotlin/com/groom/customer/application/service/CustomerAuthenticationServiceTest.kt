package com.groom.customer.application.service

import com.groom.customer.application.dto.LoginCommand
import com.groom.customer.application.dto.LogoutCommand
import com.groom.customer.common.annotation.UnitTest
import com.groom.customer.common.enums.UserRole
import com.groom.customer.common.exception.AuthenticationException
import com.groom.customer.common.exception.PermissionException
import com.groom.customer.common.exception.UserException
import com.groom.customer.domain.model.TokenCredentials
import com.groom.customer.domain.model.User
import com.groom.customer.domain.service.Authenticator
import com.groom.customer.domain.port.VerifyPasswordPort
import com.groom.customer.domain.service.UserPolicy
import com.groom.customer.outbound.repository.UserRepositoryImpl
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import java.util.Optional
import java.util.UUID

@UnitTest
class CustomerAuthenticationServiceTest :
    BehaviorSpec({

        isolationMode = IsolationMode.InstancePerLeaf

        Given("유효한 고객 로그인 정보가 주어졌을 때") {
            val userRepository = mockk<UserRepositoryImpl>()
            val verifyPasswordPort = mockk<VerifyPasswordPort>()
            val authenticator = mockk<Authenticator>()
            val userPolicy = mockk<UserPolicy>()

            val service =
                CustomerAuthenticationService(
                    userRepository = userRepository,
                    verifyPasswordPort = verifyPasswordPort,
                    authenticator = authenticator,
                    userPolicy = userPolicy,
                )

            val userId = UUID.randomUUID()
            val user =
                mockk<User>(relaxed = true) {
                    every { id } returns userId
                    every { email } returns "customer@example.com"
                    every { username } returns "고객"
                    every { role } returns UserRole.CUSTOMER
                    every { isActive } returns true
                }

            val loginCommand =
                LoginCommand(
                    email = "customer@example.com",
                    password = "password123!",
                    clientIp = "127.0.0.1",
                )

            every { userRepository.findByEmailAndRole(loginCommand.email, UserRole.CUSTOMER) } returns Optional.of(user)
            every { verifyPasswordPort.verifyPassword(user, loginCommand.password) } returns true

            val tokenCredentials =
                TokenCredentials(
                    primaryToken = "access-token",
                    secondaryToken = "refresh-token",
                    validitySeconds = 300L,
                )
            every { authenticator.createAndPersistCredentials(user, loginCommand.clientIp, any()) } returns tokenCredentials

            When("로그인을 시도하면") {
                val result = service.login(loginCommand)

                Then("로그인에 성공한다") {
                    result.refreshToken shouldBe "refresh-token"
                    result.expiresIn shouldBe 300L
                    result.tokenType shouldBe "Bearer"

                    verify(exactly = 1) { userRepository.findByEmailAndRole(loginCommand.email, UserRole.CUSTOMER) }
                    verify(exactly = 1) { verifyPasswordPort.verifyPassword(user, loginCommand.password) }
                    verify(exactly = 1) { authenticator.createAndPersistCredentials(user, loginCommand.clientIp, any()) }
                }
            }
        }

        Given("잘못된 비밀번호로 로그인할 때") {
            val userRepository = mockk<UserRepositoryImpl>()
            val verifyPasswordPort = mockk<VerifyPasswordPort>()
            val authenticator = mockk<Authenticator>()
            val userPolicy = mockk<UserPolicy>()

            val service =
                CustomerAuthenticationService(
                    userRepository = userRepository,
                    verifyPasswordPort = verifyPasswordPort,
                    authenticator = authenticator,
                    userPolicy = userPolicy,
                )

            val user =
                mockk<User>(relaxed = true) {
                    every { email } returns "customer@example.com"
                    every { role } returns UserRole.CUSTOMER
                }

            val loginCommand =
                LoginCommand(
                    email = "customer@example.com",
                    password = "wrong-password",
                    clientIp = "127.0.0.1",
                )

            every { userRepository.findByEmailAndRole(loginCommand.email, UserRole.CUSTOMER) } returns Optional.of(user)
            every { verifyPasswordPort.verifyPassword(user, loginCommand.password) } returns false

            When("로그인을 시도하면") {
                Then("예외가 발생한다") {
                    val exception =
                        shouldThrow<AuthenticationException.InvalidPassword> {
                            service.login(loginCommand)
                        }
                    exception.email shouldBe "customer@example.com"

                    verify(exactly = 1) { userRepository.findByEmailAndRole(loginCommand.email, UserRole.CUSTOMER) }
                    verify(exactly = 1) { verifyPasswordPort.verifyPassword(user, loginCommand.password) }
                    verify(exactly = 0) { authenticator.createAndPersistCredentials(any(), any()) }
                }
            }
        }

        Given("존재하지 않는 이메일로 로그인할 때") {
            val userRepository = mockk<UserRepositoryImpl>()
            val verifyPasswordPort = mockk<VerifyPasswordPort>()
            val authenticator = mockk<Authenticator>()
            val userPolicy = mockk<UserPolicy>()

            val service =
                CustomerAuthenticationService(
                    userRepository = userRepository,
                    verifyPasswordPort = verifyPasswordPort,
                    authenticator = authenticator,
                    userPolicy = userPolicy,
                )

            val loginCommand =
                LoginCommand(
                    email = "nonexistent@example.com",
                    password = "password123!",
                    clientIp = "127.0.0.1",
                )

            every { userRepository.findByEmailAndRole(loginCommand.email, UserRole.CUSTOMER) } returns Optional.empty()

            When("로그인을 시도하면") {
                Then("예외가 발생한다") {
                    val exception =
                        shouldThrow<AuthenticationException.UserNotFoundByEmail> {
                            service.login(loginCommand)
                        }
                    exception.email shouldBe "nonexistent@example.com"

                    verify(exactly = 1) { userRepository.findByEmailAndRole(loginCommand.email, UserRole.CUSTOMER) }
                    verify(exactly = 0) { verifyPasswordPort.verifyPassword(any(), any()) }
                    verify(exactly = 0) { authenticator.createAndPersistCredentials(any(), any()) }
                }
            }
        }

        Given("로그인한 고객이 로그아웃할 때") {
            val userRepository = mockk<UserRepositoryImpl>()
            val verifyPasswordPort = mockk<VerifyPasswordPort>()
            val authenticator = mockk<Authenticator>()
            val userPolicy = mockk<UserPolicy>()

            val service =
                CustomerAuthenticationService(
                    userRepository = userRepository,
                    verifyPasswordPort = verifyPasswordPort,
                    authenticator = authenticator,
                    userPolicy = userPolicy,
                )

            val userId = UUID.randomUUID()
            val user =
                mockk<User>(relaxed = true) {
                    every { id } returns userId
                    every { role } returns UserRole.CUSTOMER
                }

            val logoutCommand = LogoutCommand(userId = userId)

            every { userRepository.findById(userId) } returns Optional.of(user)
            every { userPolicy.checkUserHasRole(user, UserRole.CUSTOMER, "CustomerLogout") } just runs
            every { authenticator.revokeCredentials(user) } returns Unit

            When("로그아웃을 시도하면") {
                service.logout(logoutCommand)

                Then("로그아웃에 성공한다") {
                    verify(exactly = 1) { userRepository.findById(userId) }
                    verify(exactly = 1) { authenticator.revokeCredentials(user) }
                }
            }
        }

        Given("OWNER 역할의 사용자가 CUSTOMER 로그아웃을 시도할 때") {
            val userRepository = mockk<UserRepositoryImpl>()
            val verifyPasswordPort = mockk<VerifyPasswordPort>()
            val authenticator = mockk<Authenticator>()
            val userPolicy = mockk<UserPolicy>()

            val service =
                CustomerAuthenticationService(
                    userRepository = userRepository,
                    verifyPasswordPort = verifyPasswordPort,
                    authenticator = authenticator,
                    userPolicy = userPolicy,
                )

            val userId = UUID.randomUUID()
            val ownerUser =
                mockk<User>(relaxed = true) {
                    every { id } returns userId
                    every { role } returns UserRole.OWNER
                }

            val logoutCommand = LogoutCommand(userId = userId)

            every { userRepository.findById(userId) } returns Optional.of(ownerUser)
            every { userPolicy.checkUserHasRole(ownerUser, UserRole.CUSTOMER, "CustomerLogout") } throws
                PermissionException.AccessDenied(resource = "CustomerLogout", userId = userId)

            When("로그아웃을 시도하면") {
                Then("예외가 발생한다") {
                    val exception =
                        shouldThrow<PermissionException.AccessDenied> {
                            service.logout(logoutCommand)
                        }
                    exception.resource shouldBe "CustomerLogout"
                    exception.userId shouldBe userId

                    verify(exactly = 1) { userRepository.findById(userId) }
                    verify(exactly = 0) { authenticator.revokeCredentials(any()) }
                }
            }
        }

        Given("존재하지 않는 사용자가 로그아웃을 시도할 때") {
            val userRepository = mockk<UserRepositoryImpl>()
            val verifyPasswordPort = mockk<VerifyPasswordPort>()
            val authenticator = mockk<Authenticator>()
            val userPolicy = mockk<UserPolicy>()

            val service =
                CustomerAuthenticationService(
                    userRepository = userRepository,
                    verifyPasswordPort = verifyPasswordPort,
                    authenticator = authenticator,
                    userPolicy = userPolicy,
                )

            val userId = UUID.randomUUID()
            val logoutCommand = LogoutCommand(userId = userId)

            every { userRepository.findById(userId) } returns Optional.empty()

            When("로그아웃을 시도하면") {
                Then("예외가 발생한다") {
                    val exception =
                        shouldThrow<UserException.UserNotFound> {
                            service.logout(logoutCommand)
                        }
                    exception.userId shouldBe userId

                    verify(exactly = 1) { userRepository.findById(userId) }
                }
            }
        }
    })
