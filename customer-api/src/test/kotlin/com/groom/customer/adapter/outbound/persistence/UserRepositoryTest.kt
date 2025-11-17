package com.groom.customer.adapter.outbound.persistence

import com.groom.customer.common.TransactionApplier
import com.groom.customer.common.annotation.IntegrationTest
import com.groom.customer.domain.model.User
import com.groom.customer.domain.model.UserRole
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException

@IntegrationTest
@DisplayName("UserRepositoryImpl CRUD 테스트")
class UserRepositoryTest {
    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var transactionApplier: TransactionApplier

    @AfterEach
    fun cleanup() {
        userRepository.deleteAll()
        userRepository.flush()
    }

    @Nested
    @DisplayName("Create 테스트")
    inner class CreateTest {
        @Test
        @DisplayName("사용자를 정상적으로 저장할 수 있다")
        fun `should save user successfully`() {
            // given
            val user =
                User(
                    username = "홍길동",
                    email = "hong@example.com",
                    passwordHash = "hashedPassword123",
                    role = UserRole.CUSTOMER,
                )

            // when
            val savedUser = userRepository.save(user)
            userRepository.flush()

            // then
            val foundUser =
                transactionApplier.applyPrimaryTransaction {
                    userRepository.findById(savedUser.id)
                }
            assertThat(foundUser).isPresent
            assertThat(foundUser.get().id).isNotNull
            assertThat(foundUser.get().username).isEqualTo("홍길동")
            assertThat(foundUser.get().email).isEqualTo("hong@example.com")
            assertThat(foundUser.get().role).isEqualTo(UserRole.CUSTOMER)
            assertThat(foundUser.get().isActive).isTrue
        }

        @Test
        @DisplayName("중복된 이메일로 저장 시 예외가 발생한다")
        fun `should throw exception when saving user with duplicate email`() {
            // given
            val user1 =
                User(
                    username = "홍길동",
                    email = "duplicate@example.com",
                    passwordHash = "password1",
                    role = UserRole.CUSTOMER,
                )
            userRepository.save(user1)
            userRepository.flush()

            val user2 =
                User(
                    username = "김철수",
                    email = "duplicate@example.com",
                    passwordHash = "password2",
                    role = UserRole.CUSTOMER,
                )

            // when & then
            org.junit.jupiter.api.assertThrows<DataIntegrityViolationException> {
                userRepository.saveAndFlush(user2)
            }
        }
    }

    @Nested
    @DisplayName("Read 테스트")
    inner class ReadTest {
        @Test
        @DisplayName("ID로 사용자를 조회할 수 있다")
        fun `should find user by id`() {
            // given
            val user =
                User(
                    username = "홍길동",
                    email = "hong@example.com",
                    passwordHash = "hashedPassword",
                    role = UserRole.CUSTOMER,
                )
            val savedUser = userRepository.save(user)
            userRepository.flush()

            // when
            val foundUser =
                transactionApplier.applyPrimaryTransaction {
                    userRepository.findById(savedUser.id)
                }

            // then
            assertThat(foundUser).isPresent
            assertThat(foundUser.get().username).isEqualTo("홍길동")
        }

        @Test
        @DisplayName("이메일로 사용자를 조회할 수 있다")
        fun `should find user by email`() {
            // given
            val user =
                User(
                    username = "홍길동",
                    email = "hong@example.com",
                    passwordHash = "hashedPassword",
                    role = UserRole.CUSTOMER,
                )
            userRepository.save(user)
            userRepository.flush()

            // when
            val foundUser =
                transactionApplier.applyPrimaryTransaction {
                    userRepository.findByEmail("hong@example.com")
                }

            // then
            assertThat(foundUser).isPresent
            assertThat(foundUser.get().username).isEqualTo("홍길동")
        }

        @Test
        @DisplayName("이메일과 역할로 사용자를 조회할 수 있다")
        fun `should find user by email and role`() {
            // given
            val customer =
                User(
                    username = "고객",
                    email = "user@example.com",
                    passwordHash = "password",
                    role = UserRole.CUSTOMER,
                )
            val owner =
                User(
                    username = "사장님",
                    email = "user@example.com",
                    passwordHash = "password",
                    role = UserRole.OWNER,
                )
            userRepository.save(customer)
            userRepository.save(owner)
            userRepository.flush()

            // when
            val foundCustomer =
                transactionApplier.applyPrimaryTransaction {
                    userRepository.findByEmailAndRole("user@example.com", UserRole.CUSTOMER)
                }
            val foundOwner =
                transactionApplier.applyPrimaryTransaction {
                    userRepository.findByEmailAndRole("user@example.com", UserRole.OWNER)
                }

            // then
            assertThat(foundCustomer).isPresent
            assertThat(foundCustomer.get().username).isEqualTo("고객")
            assertThat(foundOwner).isPresent
            assertThat(foundOwner.get().username).isEqualTo("사장님")
        }

        @Test
        @DisplayName("존재하지 않는 사용자 조회 시 빈 Optional을 반환한다")
        fun `should return empty optional when user not found`() {
            // when
            val foundUser =
                transactionApplier.applyPrimaryTransaction {
                    userRepository.findByEmail("notexist@example.com")
                }

            // then
            assertThat(foundUser).isEmpty
        }

        @Test
        @DisplayName("사용자명 존재 여부를 확인할 수 있다")
        fun `should check if username exists`() {
            // given
            val user =
                User(
                    username = "홍길동",
                    email = "hong@example.com",
                    passwordHash = "password",
                    role = UserRole.CUSTOMER,
                )
            userRepository.save(user)
            userRepository.flush()

            // when & then
            val exists =
                transactionApplier.applyPrimaryTransaction {
                    userRepository.existsByUsername("홍길동")
                }
            val notExists =
                transactionApplier.applyPrimaryTransaction {
                    userRepository.existsByUsername("김철수")
                }
            assertThat(exists).isTrue
            assertThat(notExists).isFalse
        }

        @Test
        @DisplayName("이메일 존재 여부를 확인할 수 있다")
        fun `should check if email exists`() {
            // given
            val user =
                User(
                    username = "홍길동",
                    email = "hong@example.com",
                    passwordHash = "password",
                    role = UserRole.CUSTOMER,
                )
            userRepository.save(user)
            userRepository.flush()

            // when & then
            val exists =
                transactionApplier.applyPrimaryTransaction {
                    userRepository.existsByEmail("hong@example.com")
                }
            val notExists =
                transactionApplier.applyPrimaryTransaction {
                    userRepository.existsByEmail("other@example.com")
                }
            assertThat(exists).isTrue
            assertThat(notExists).isFalse
        }

        @Test
        @DisplayName("이메일과 역할로 사용자 존재 여부를 확인할 수 있다")
        fun `should check if user exists by email and role`() {
            // given
            val customer =
                User(
                    username = "고객",
                    email = "user@example.com",
                    passwordHash = "password",
                    role = UserRole.CUSTOMER,
                )
            userRepository.save(customer)
            userRepository.flush()

            // when & then
            val customerExists =
                transactionApplier.applyPrimaryTransaction {
                    userRepository.existsByEmailIsAndRoleIs("user@example.com", UserRole.CUSTOMER)
                }
            val ownerExists =
                transactionApplier.applyPrimaryTransaction {
                    userRepository.existsByEmailIsAndRoleIs("user@example.com", UserRole.OWNER)
                }
            assertThat(customerExists).isTrue
            assertThat(ownerExists).isFalse
        }
    }

    @Nested
    @DisplayName("Update 테스트")
    inner class UpdateTest {
        @Test
        @DisplayName("사용자 정보를 확인할 수 있다")
        fun `should verify user information`() {
            // given
            val user =
                User(
                    username = "홍길동",
                    email = "update@example.com",
                    passwordHash = "password123",
                    role = UserRole.CUSTOMER,
                )
            val savedUser = userRepository.save(user)
            userRepository.flush()

            // when
            val foundUser =
                transactionApplier.applyPrimaryTransaction {
                    userRepository.findById(savedUser.id)
                }

            // then
            assertThat(foundUser).isPresent
            foundUser.ifPresent {
                assertThat(it.username).isEqualTo("홍길동")
                assertThat(it.email).isEqualTo("update@example.com")
                assertThat(it.role).isEqualTo(UserRole.CUSTOMER)
                assertThat(it.isActive).isTrue
            }
        }

        @Test
        @DisplayName("비활성화된 사용자를 저장할 수 있다")
        fun `should save deactivated user`() {
            // given
            val user =
                User(
                    username = "비활성",
                    email = "inactive2@example.com",
                    passwordHash = "password",
                    role = UserRole.CUSTOMER,
                    isActive = false,
                )

            // when
            val savedUser = userRepository.save(user)
            userRepository.flush()

            // then
            val foundUser =
                transactionApplier.applyPrimaryTransaction {
                    userRepository.findById(savedUser.id)
                }
            assertThat(foundUser).isPresent
            assertThat(foundUser.get().isActive).isFalse
        }
    }

    @Nested
    @DisplayName("Delete 테스트")
    inner class DeleteTest {
        @Test
        @DisplayName("사용자를 삭제할 수 있다")
        fun `should delete user`() {
            // given
            val user =
                User(
                    username = "삭제대상",
                    email = "delete@example.com",
                    passwordHash = "password",
                    role = UserRole.CUSTOMER,
                )
            val savedUser = userRepository.save(user)
            userRepository.flush()
            val userId = savedUser.id

            // when
            userRepository.deleteById(userId)
            userRepository.flush() // 강제로 DB에 반영

            // then
            val exists =
                transactionApplier.applyPrimaryTransaction {
                    userRepository.existsById(userId)
                }
            assertThat(exists).isFalse
        }

        @Test
        @DisplayName("ID로 사용자를 삭제할 수 있다")
        fun `should delete user by id`() {
            // given
            val user =
                User(
                    username = "홍길동",
                    email = "hong@example.com",
                    passwordHash = "password",
                    role = UserRole.CUSTOMER,
                )
            val savedUser = userRepository.save(user)
            userRepository.flush()

            // when
            userRepository.deleteById(savedUser.id)
            userRepository.flush()

            // then
            val exists =
                transactionApplier.applyPrimaryTransaction {
                    userRepository.existsById(savedUser.id)
                }
            assertThat(exists).isFalse
        }
    }

    @Nested
    @DisplayName("Batch 테스트")
    inner class BatchTest {
        @Test
        @DisplayName("여러 사용자를 한 번에 저장할 수 있다")
        fun `should save multiple users in batch`() {
            // given
            val users =
                listOf(
                    User("홍길동", "hong@example.com", "password1", UserRole.CUSTOMER),
                    User("김철수", "kim@example.com", "password2", UserRole.CUSTOMER),
                    User("이영희", "lee@example.com", "password3", UserRole.OWNER),
                )

            // when
            val savedUsers = userRepository.saveAll(users)
            userRepository.flush()

            // then
            assertThat(savedUsers).hasSize(3)
            val savedIds = savedUsers.map { it.id }
            val allExist =
                transactionApplier.applyPrimaryTransaction {
                    savedIds.all { userRepository.existsById(it) }
                }
            assertThat(allExist).isTrue
        }

        @Test
        @DisplayName("배치로 저장한 사용자를 조회할 수 있다")
        fun `should find batch saved users`() {
            // given
            val users =
                listOf(
                    User("홍길동", "hong@example.com", "password1", UserRole.CUSTOMER),
                    User("김철수", "kim@example.com", "password2", UserRole.CUSTOMER),
                )
            val savedUsers = userRepository.saveAll(users)
            userRepository.flush()

            // when
            val savedIds = savedUsers.map { it.id }
            val foundUsers =
                transactionApplier.applyPrimaryTransaction {
                    userRepository.findAllById(savedIds)
                }

            // then
            assertThat(foundUsers).hasSize(2)
            assertThat(foundUsers.map { it.username }).containsExactlyInAnyOrder("홍길동", "김철수")
        }

        @Test
        @DisplayName("배치로 저장한 사용자들을 삭제할 수 있다")
        fun `should delete batch saved users`() {
            // given
            val users =
                listOf(
                    User("홍길동", "hong@example.com", "password1", UserRole.CUSTOMER),
                    User("김철수", "kim@example.com", "password2", UserRole.CUSTOMER),
                )
            val savedUsers = userRepository.saveAll(users)
            userRepository.flush()
            val savedIds = savedUsers.map { it.id }

            // when
            userRepository.deleteAllById(savedIds)
            userRepository.flush()

            // then
            val allDeleted =
                transactionApplier.applyPrimaryTransaction {
                    savedIds.all { !userRepository.existsById(it) }
                }
            assertThat(allDeleted).isTrue
        }
    }
}
