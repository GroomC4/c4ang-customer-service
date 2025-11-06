package com.groom.customer.domain.service

import com.groom.customer.common.annotation.UnitTest
import com.groom.customer.common.enums.UserRole
import com.groom.customer.common.exception.UserException
import com.groom.customer.domain.model.User
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.Optional

@UnitTest
class UserPolicyTest :
    ShouldSpec({

        val userReader = mockk<UserReader>()
        val userPolicy = UserPolicy(userReader)

        context("checkAlreadyRegister") {
            should("동일한 이메일과 역할로 등록된 사용자가 없으면 예외가 발생하지 않는다") {
                // given
                val email = "new@example.com"
                val role = UserRole.CUSTOMER
                every { userReader.findByEmailAndRole(email, role) } returns Optional.empty()

                // when & then
                shouldNotThrow<UserException.DuplicateEmail> {
                    userPolicy.checkAlreadyRegister(email, role)
                }

                verify(exactly = 1) { userReader.findByEmailAndRole(email, role) }
            }

            should("동일한 이메일과 역할(CUSTOMER)로 이미 등록된 사용자가 있으면 예외가 발생한다") {
                // given
                val email = "existing@example.com"
                val role = UserRole.CUSTOMER
                val existingUser =
                    mockk<User> {
                        every { this@mockk.role } returns UserRole.CUSTOMER
                    }
                every { userReader.findByEmailAndRole(email, role) } returns Optional.of(existingUser)

                // when & then
                val exception =
                    shouldThrow<UserException.DuplicateEmail> {
                        userPolicy.checkAlreadyRegister(email, role)
                    }

                exception.message shouldContain "이미 존재하는 이메일입니다"
                verify(exactly = 1) { userReader.findByEmailAndRole(email, role) }
            }

            should("동일한 이메일과 역할(OWNER)로 이미 등록된 사용자가 있으면 예외가 발생한다") {
                // given
                val email = "owner@example.com"
                val role = UserRole.OWNER
                val ownerUser =
                    mockk<User> {
                        every { this@mockk.role } returns UserRole.OWNER
                    }
                every { userReader.findByEmailAndRole(email, role) } returns Optional.of(ownerUser)

                // when & then
                val exception =
                    shouldThrow<UserException.DuplicateEmail> {
                        userPolicy.checkAlreadyRegister(email, role)
                    }

                exception.message shouldContain "이미 존재하는 이메일입니다"
                verify(exactly = 1) { userReader.findByEmailAndRole(email, role) }
            }

            should("동일한 이메일이지만 다른 역할로 등록하는 경우 예외가 발생하지 않는다") {
                // given
                val email = "same@example.com"
                // CUSTOMER로는 이미 등록되어 있지만, OWNER로는 등록되어 있지 않음
                every { userReader.findByEmailAndRole(email, UserRole.OWNER) } returns Optional.empty()

                // when & then
                shouldNotThrow<UserException.DuplicateEmail> {
                    userPolicy.checkAlreadyRegister(email, UserRole.OWNER)
                }

                verify(exactly = 1) { userReader.findByEmailAndRole(email, UserRole.OWNER) }
            }
        }
    })
