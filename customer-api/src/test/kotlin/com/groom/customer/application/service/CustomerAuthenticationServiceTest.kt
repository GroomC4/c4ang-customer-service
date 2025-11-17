package com.groom.customer.application.service

import com.groom.customer.application.dto.LoginCommand
import com.groom.customer.application.dto.LogoutCommand
import com.groom.customer.common.annotation.UnitTest
import com.groom.customer.common.exception.AuthenticationException
import com.groom.customer.common.exception.PermissionException
import com.groom.customer.common.exception.UserException
import com.groom.customer.domain.model.TokenCredentials
import com.groom.customer.domain.model.User
import com.groom.customer.domain.model.UserRole
import com.groom.customer.domain.service.Authenticator
import com.groom.customer.domain.service.UserPolicy
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import java.util.UUID

@UnitTest
class CustomerAuthenticationServiceTest :
    BehaviorSpec({

        isolationMode = IsolationMode.InstancePerLeaf

        Given("유효한 고객 로그인 정보가 주어졌을 때") {
            val authenticator = mockk<Authenticator>()
            val userPolicy = mockk<UserPolicy>()

            val service =
                CustomerAuthenticationService(
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

            every { userPolicy.loadActiveCustomerByEmail(loginCommand.email) } returns user
            every { userPolicy.validatePassword(user, loginCommand.password, loginCommand.email) } just runs

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

                    verify(exactly = 1) { userPolicy.loadActiveCustomerByEmail(loginCommand.email) }
                    verify(exactly = 1) { userPolicy.validatePassword(user, loginCommand.password, loginCommand.email) }
                    verify(exactly = 1) { authenticator.createAndPersistCredentials(user, loginCommand.clientIp, any()) }
                }
            }
        }

        Given("잘못된 비밀번호로 로그인할 때") {
            val authenticator = mockk<Authenticator>()
            val userPolicy = mockk<UserPolicy>()

            val service =
                CustomerAuthenticationService(
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

            every { userPolicy.loadActiveCustomerByEmail(loginCommand.email) } returns user
            every { userPolicy.validatePassword(user, loginCommand.password, loginCommand.email) } throws
                AuthenticationException.InvalidPassword(email = loginCommand.email)

            When("로그인을 시도하면") {
                Then("예외가 발생한다") {
                    val exception =
                        shouldThrow<AuthenticationException.InvalidPassword> {
                            service.login(loginCommand)
                        }
                    exception.email shouldBe "customer@example.com"

                    verify(exactly = 1) { userPolicy.loadActiveCustomerByEmail(loginCommand.email) }
                    verify(exactly = 1) { userPolicy.validatePassword(user, loginCommand.password, loginCommand.email) }
                    verify(exactly = 0) { authenticator.createAndPersistCredentials(any(), any()) }
                }
            }
        }

        Given("존재하지 않는 이메일로 로그인할 때") {
            val authenticator = mockk<Authenticator>()
            val userPolicy = mockk<UserPolicy>()

            val service =
                CustomerAuthenticationService(
                    authenticator = authenticator,
                    userPolicy = userPolicy,
                )

            val loginCommand =
                LoginCommand(
                    email = "nonexistent@example.com",
                    password = "password123!",
                    clientIp = "127.0.0.1",
                )

            every { userPolicy.loadActiveCustomerByEmail(loginCommand.email) } throws
                AuthenticationException.UserNotFoundByEmail(email = loginCommand.email)

            When("로그인을 시도하면") {
                Then("예외가 발생한다") {
                    val exception =
                        shouldThrow<AuthenticationException.UserNotFoundByEmail> {
                            service.login(loginCommand)
                        }
                    exception.email shouldBe "nonexistent@example.com"

                    verify(exactly = 1) { userPolicy.loadActiveCustomerByEmail(loginCommand.email) }
                    verify(exactly = 0) { authenticator.createAndPersistCredentials(any(), any()) }
                }
            }
        }

        Given("로그인한 고객이 로그아웃할 때") {
            val authenticator = mockk<Authenticator>()
            val userPolicy = mockk<UserPolicy>()

            val service =
                CustomerAuthenticationService(
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

            every { userPolicy.loadCustomerById(userId) } returns user
            every { authenticator.revokeCredentials(user) } returns Unit

            When("로그아웃을 시도하면") {
                service.logout(logoutCommand)

                Then("로그아웃에 성공한다") {
                    verify(exactly = 1) { userPolicy.loadCustomerById(userId) }
                    verify(exactly = 1) { authenticator.revokeCredentials(user) }
                }
            }
        }

        Given("OWNER 역할의 사용자가 CUSTOMER 로그아웃을 시도할 때") {
            val authenticator = mockk<Authenticator>()
            val userPolicy = mockk<UserPolicy>()

            val service =
                CustomerAuthenticationService(
                    authenticator = authenticator,
                    userPolicy = userPolicy,
                )

            val userId = UUID.randomUUID()

            val logoutCommand = LogoutCommand(userId = userId)

            every { userPolicy.loadCustomerById(userId) } throws
                PermissionException.AccessDenied(resource = "CustomerLogout", userId = userId)

            When("로그아웃을 시도하면") {
                Then("예외가 발생한다") {
                    val exception =
                        shouldThrow<PermissionException.AccessDenied> {
                            service.logout(logoutCommand)
                        }
                    exception.resource shouldBe "CustomerLogout"
                    exception.userId shouldBe userId

                    verify(exactly = 1) { userPolicy.loadCustomerById(userId) }
                    verify(exactly = 0) { authenticator.revokeCredentials(any()) }
                }
            }
        }

        Given("존재하지 않는 사용자가 로그아웃을 시도할 때") {
            val authenticator = mockk<Authenticator>()
            val userPolicy = mockk<UserPolicy>()

            val service =
                CustomerAuthenticationService(
                    authenticator = authenticator,
                    userPolicy = userPolicy,
                )

            val userId = UUID.randomUUID()
            val logoutCommand = LogoutCommand(userId = userId)

            every { userPolicy.loadCustomerById(userId) } throws
                UserException.UserNotFound(userId = userId)

            When("로그아웃을 시도하면") {
                Then("예외가 발생한다") {
                    val exception =
                        shouldThrow<UserException.UserNotFound> {
                            service.logout(logoutCommand)
                        }
                    exception.userId shouldBe userId

                    verify(exactly = 1) { userPolicy.loadCustomerById(userId) }
                }
            }
        }
    })
