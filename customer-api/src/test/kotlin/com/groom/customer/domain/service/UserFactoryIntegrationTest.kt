package com.groom.customer.domain.service

import com.groom.customer.common.TransactionApplier
import com.groom.customer.common.annotation.IntegrationTest
import com.groom.customer.domain.model.Address
import com.groom.customer.domain.model.Email
import com.groom.customer.domain.model.PhoneNumber
import com.groom.customer.domain.model.Username
import com.groom.customer.outbound.repository.UserProfileRepositoryImpl
import com.groom.customer.outbound.repository.UserRepositoryImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.util.UUID
import kotlin.jvm.optionals.getOrNull

@IntegrationTest
@SpringBootTest
class UserFactoryIntegrationTest {
    @Autowired
    private lateinit var userFactory: UserFactory

    @Autowired
    private lateinit var userRepository: UserRepositoryImpl

    @Autowired
    private lateinit var userProfileRepository: UserProfileRepositoryImpl

    @Autowired
    private lateinit var transactionApplier: TransactionApplier

    // 테스트 데이터 격리를 위한 추적 리스트
    private val createdUserIds = mutableListOf<UUID>()

    @BeforeEach
    fun setUp() {
        // 테스트 시작 전 추적 리스트 초기화
        createdUserIds.clear()
    }

    @AfterEach
    fun tearDown() {
        // 테스트 종료 후 생성된 데이터만 정리
        createdUserIds.forEach { userId ->
            transactionApplier.applyPrimaryTransaction {
                userRepository.findById(userId).ifPresent { user ->
                    // User를 삭제하면 CascadeType.ALL로 인해 UserProfile도 함께 삭제됨
                    userRepository.delete(user)
                }
            }
        }
        // 추적 리스트 비우기
        createdUserIds.clear()
    }

    @Test
    fun `User를 저장하면 UserProfile도 함께 저장된다`() {
        // given
        val username = Username.from("홍길동")
        val email = Email.from("hong@example.com")
        val passwordHash = "hashed-password"
        val defaultAddress = Address.from("서울시 강남구 테헤란로 123")
        val defaultPhoneNumber = PhoneNumber.from("010-1234-5678")

        val user =
            userFactory.createNewCustomer(
                username = username,
                email = email,
                passwordHash = passwordHash,
                defaultAddress = defaultAddress,
                defaultPhoneNumber = defaultPhoneNumber,
            )

        // when
        val savedUser = userRepository.save(user)
        createdUserIds.add(savedUser.id) // 추적 리스트에 추가

        // then - User가 저장되었는지 확인
        assertThat(savedUser.id).isNotNull()
        assertThat(savedUser.id).isNotNull()
        assertThat(savedUser.username).isEqualTo(username.value)
        assertThat(savedUser.email).isEqualTo(email.value)
        assertThat(savedUser.profile).isNotNull()

        // then - UserProfile도 함께 저장되었는지 확인
        val savedProfile = savedUser.profile!!
        assertThat(savedProfile.id).isNotNull()
        assertThat(savedProfile.fullName).isEqualTo(username.value)
        assertThat(savedProfile.phoneNumber).isEqualTo(defaultPhoneNumber.value)
        assertThat(savedProfile.contactEmail).isEqualTo(email.value)
        assertThat(savedProfile.defaultAddress).isEqualTo(defaultAddress.value)

        // then - 양방향 관계가 올바르게 설정되었는지 확인
        assertThat(savedProfile.user).isEqualTo(savedUser)
    }

    @Test
    fun `DB에서 User를 조회하면 UserProfile이 함께 로드된다`() {
        // given
        val username = Username.from("김철수")
        val email = Email.from("kim@example.com")
        val passwordHash = "hashed-password"
        val defaultAddress = Address.from("서울시")
        val defaultPhoneNumber = PhoneNumber.from("010-1111-1111")

        val user =
            userFactory.createNewCustomer(
                username = username,
                email = email,
                passwordHash = passwordHash,
                defaultAddress = defaultAddress,
                defaultPhoneNumber = defaultPhoneNumber,
            )

        val savedUser = userRepository.save(user)
        val userId = savedUser.id
        createdUserIds.add(userId) // 추적 리스트에 추가

        // when - DB에서 다시 조회
        val foundUser =
            transactionApplier
                .applyPrimaryTransaction { userRepository.findById(userId) }
                .orElseThrow()

        // then - UserProfile이 함께 로드되었는지 확인
        assertThat(foundUser.profile).isNotNull()
        assertThat(foundUser.profile!!.fullName).isEqualTo(username.value)
        assertThat(foundUser.profile!!.phoneNumber).isEqualTo(defaultPhoneNumber.value)
        assertThat(foundUser.profile!!.user).isEqualTo(foundUser)
    }

    @Test
    fun `UserProfile을 통해 User를 조회할 수 있다`() {
        // given
        val username = Username.from("이영희")
        val email = Email.from("lee@example.com")
        val passwordHash = "hashed-password"
        val defaultAddress = Address.from("경기도")
        val defaultPhoneNumber = PhoneNumber.from("010-2222-2222")

        val user =
            userFactory.createNewCustomer(
                username = username,
                email = email,
                passwordHash = passwordHash,
                defaultAddress = defaultAddress,
                defaultPhoneNumber = defaultPhoneNumber,
            )

        val savedUser = userRepository.save(user)
        val userId = savedUser.id
        createdUserIds.add(userId) // 추적 리스트에 추가

        // when - UserProfile을 조회
        val foundProfile = userProfileRepository.findByUser_Id(userId).orElseThrow()

        // then - UserProfile에서 User를 참조할 수 있는지 확인
        assertThat(foundProfile.user).isNotNull()
        assertThat(foundProfile.user!!.id).isEqualTo(userId)
        assertThat(foundProfile.user!!.username).isEqualTo(username.value)
        assertThat(foundProfile.user!!.email).isEqualTo(email.value)
    }

    @Test
    fun `p_user와 p_user_profile 테이블에 각각 row가 생성된다`() {
        // given
        val username = Username.from("박민수")
        val email = Email.from("park@example.com")
        val passwordHash = "hashed-password"
        val defaultAddress = Address.from("부산시")
        val defaultPhoneNumber = PhoneNumber.from("010-3333-3333")

        val user =
            userFactory.createNewCustomer(
                username = username,
                email = email,
                passwordHash = passwordHash,
                defaultAddress = defaultAddress,
                defaultPhoneNumber = defaultPhoneNumber,
            )

        // when
        val savedUser = userRepository.save(user)
        createdUserIds.add(savedUser.id) // 추적 리스트에 추가

        // then - p_user 테이블에 저장 확인 (Primary DB에서 조회)
        transactionApplier.applyPrimaryTransaction {
            val selectedUser = userRepository.findById(savedUser.id).getOrNull()
            assertThat(selectedUser).isNotNull()

            // then - p_user_profile 테이블에 저장 확인
            val selectedUserProfile = userProfileRepository.findByUser_Id(selectedUser!!.id!!)
            assertThat(selectedUserProfile.getOrNull()).isNotNull()

            // then - UserProfile의 user_id가 User의 id를 참조하는지 확인
            val profile = userProfileRepository.findByUser_Id(savedUser.id).orElseThrow()
            assertThat(profile.user!!.id).isEqualTo(savedUser.id)
        }
    }

    @Test
    fun `여러 User를 저장해도 각각의 UserProfile이 올바르게 연결된다`() {
        // given
        val users =
            listOf(
                Triple(
                    Username.from("최수진"),
                    Email.from("choi@example.com"),
                    PhoneNumber.from("010-4444-4444"),
                ),
                Triple(
                    Username.from("정다은"),
                    Email.from("jung@example.com"),
                    PhoneNumber.from("010-5555-5555"),
                ),
            )

        // when
        val savedUsers =
            users.map { (username, email, phoneNumber) ->
                val user =
                    userFactory.createNewCustomer(
                        username = username,
                        email = email,
                        passwordHash = "hashed-password",
                        defaultAddress = Address.from("서울시"),
                        defaultPhoneNumber = phoneNumber,
                    )
                val savedUser = userRepository.save(user)
                createdUserIds.add(savedUser.id) // 추적 리스트에 추가
                savedUser
            }

        // then
        savedUsers.forEach { savedUser ->
            assertThat(savedUser.profile).isNotNull()
            assertThat(savedUser.profile!!.user).isEqualTo(savedUser)

            // DB에서 다시 조회해서 확인
            val foundProfile = userProfileRepository.findByUser_Id(savedUser.id).orElseThrow()
            assertThat(foundProfile.user!!.id).isEqualTo(savedUser.id)
            assertThat(foundProfile.fullName).isEqualTo(savedUser.username)
        }
    }
}
