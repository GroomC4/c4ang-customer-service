package com.groom.customer.application.service

import com.groom.customer.application.dto.RegisterCustomerCommand
import com.groom.customer.common.AbstractIntegrationTest
import com.groom.customer.common.enums.UserRole
import com.groom.customer.common.exception.UserException
import com.groom.customer.outbound.repository.UserRepositoryImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class RegisterCustomerServiceIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    private lateinit var registerCustomerService: RegisterCustomerService

    @Autowired
    private lateinit var userRepository: UserRepositoryImpl

    // 각 테스트에서 생성한 이메일 목록을 추적
    private val createdEmails = mutableListOf<String>()

    @BeforeEach
    fun setUp() {
        createdEmails.clear()
    }

    @AfterEach
    fun tearDown() {
        // 이 테스트에서 생성한 사용자만 삭제
        createdEmails.forEach { email ->
            userRepository.findByEmail(email).ifPresent { user ->
                // UserProfile은 cascade로 자동 삭제됨
                userRepository.delete(user)
            }
        }
    }

    // 테스트에서 사용자 생성 시 이메일을 추적하는 헬퍼 함수
    private fun trackEmail(email: String) {
        createdEmails.add(email)
    }

    @Test
    fun `정상적인 회원가입 요청 시 User와 UserProfile이 모두 저장되고 결과를 반환한다`() {
        // given
        val command =
            RegisterCustomerCommand(
                username = "홍길동",
                email = "hong@example.com",
                rawPassword = "P@ssw0rd!",
                defaultAddress = "서울특별시 송파구 올림픽로 300",
                defaultPhoneNumber = "010-1234-5678",
            )
        trackEmail(command.email)

        // when
        val result = registerCustomerService.register(command)

        // then - 반환값 검증
        assertThat(result).isNotNull
        assertThat(result.userId).isNotNull
        assertThat(result.username).isEqualTo("홍길동")
        assertThat(result.email).isEqualTo("hong@example.com")
        assertThat(result.role).isEqualTo(UserRole.CUSTOMER)
        assertThat(result.isActive).isTrue
        assertThat(result.createdAt).isNotNull

        // then - DB에 실제 저장되었는지 검증
        val savedUser = userRepository.findByEmail("hong@example.com").orElseThrow()
        assertThat(savedUser.id).isNotNull
        assertThat(savedUser.username).isEqualTo("홍길동")
        assertThat(savedUser.email).isEqualTo("hong@example.com")
        assertThat(savedUser.role).isEqualTo(UserRole.CUSTOMER)
        assertThat(savedUser.isActive).isTrue
        assertThat(savedUser.passwordHash).startsWith("\$2a\$") // Bcrypt 해시 형식 검증
        assertThat(savedUser.createdAt).isNotNull

        // then - UserProfile도 함께 저장되었는지 검증
        assertThat(savedUser.profile).isNotNull
        assertThat(savedUser.profile!!.fullName).isEqualTo("홍길동")
        assertThat(savedUser.profile!!.phoneNumber).isEqualTo("010-1234-5678")
        assertThat(savedUser.profile!!.contactEmail).isEqualTo("hong@example.com")
        assertThat(savedUser.profile!!.defaultAddress).isEqualTo("서울특별시 송파구 올림픽로 300")
        assertThat(savedUser.profile!!.user).isEqualTo(savedUser)
    }

    @Test
    fun `동일한 이메일로 중복 가입 시도 시 UserException DuplicateEmail이 발생한다`() {
        // given - 첫 번째 사용자 등록
        val firstCommand =
            RegisterCustomerCommand(
                username = "박민수",
                email = "park@example.com",
                rawPassword = "password123!",
                defaultAddress = "부산시",
                defaultPhoneNumber = "010-3333-3333",
            )
        trackEmail(firstCommand.email)
        registerCustomerService.register(firstCommand)

        // when - 동일 이메일로 다시 가입 시도
        val duplicateCommand =
            RegisterCustomerCommand(
                username = "최다은",
                email = "park@example.com", // 동일 이메일
                rawPassword = "different123!",
                defaultAddress = "인천시",
                defaultPhoneNumber = "010-4444-4444",
            )

        // then - UserException.DuplicateEmail 예외 발생
        val exception =
            assertThrows<UserException.DuplicateEmail> {
                registerCustomerService.register(duplicateCommand)
            }
        assertThat(exception.email).isEqualTo("park@example.com")
    }

    @Test
    fun `잘못된 이메일 형식으로 가입 시도 시 IllegalArgumentException이 발생한다`() {
        // given
        val command =
            RegisterCustomerCommand(
                username = "정수진",
                email = "invalid-email-format", // 잘못된 형식
                rawPassword = "password123!",
                defaultAddress = "대구시",
                defaultPhoneNumber = "010-5555-5555",
            )

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                registerCustomerService.register(command)
            }
        assertThat(exception.message).isEqualTo("이메일 형식이 올바르지 않습니다.")
    }

    // Note: PhoneNumber 값 객체는 primary constructor를 사용하므로 검증이 발생하지 않음
    // RegisterCustomerService에서 PhoneNumber(value) 대신 PhoneNumber.from(value)를 사용해야 검증됨

    @Test
    fun `여러 사용자를 순차적으로 등록할 수 있다`() {
        // given
        val commands =
            listOf(
                RegisterCustomerCommand(
                    username = "김철수",
                    email = "kim-multi-test@example.com",
                    rawPassword = "password1",
                    defaultAddress = "서울시",
                    defaultPhoneNumber = "010-1111-1111",
                ),
                RegisterCustomerCommand(
                    username = "이영희",
                    email = "lee-multi-test@example.com",
                    rawPassword = "password2",
                    defaultAddress = "경기도",
                    defaultPhoneNumber = "010-2222-2222",
                ),
                RegisterCustomerCommand(
                    username = "강호동",
                    email = "kang-multi-test@example.com",
                    rawPassword = "password3",
                    defaultAddress = "부산시",
                    defaultPhoneNumber = "010-6666-6666",
                ),
            )
        commands.forEach { trackEmail(it.email) }

        // when
        val results = commands.map { registerCustomerService.register(it) }

        // then
        assertThat(results).hasSize(3)
        results.forEachIndexed { index, result ->
            assertThat(result.username).isEqualTo(commands[index].username)
            assertThat(result.email).isEqualTo(commands[index].email)
            assertThat(result.role).isEqualTo(UserRole.CUSTOMER)
            assertThat(result.isActive).isTrue
        }

        // then - DB에 모두 저장되었는지 확인
        commands.forEach { command ->
            val savedUser = userRepository.findByEmail(command.email).orElseThrow()
            assertThat(savedUser.username).isEqualTo(command.username)
            assertThat(savedUser.profile).isNotNull
            assertThat(savedUser.profile!!.user).isEqualTo(savedUser)
        }
    }

    @Test
    fun `DB에서 자동 생성된 externalId가 올바르게 반환된다`() {
        // given
        val command =
            RegisterCustomerCommand(
                username = "유재석",
                email = "yoo-external-id-test@example.com",
                rawPassword = "password123!",
                defaultAddress = "서울시",
                defaultPhoneNumber = "010-7777-7777",
            )
        trackEmail(command.email)

        // when
        val result = registerCustomerService.register(command)

        // then - 반환된 userId가 UUID 형식인지 검증
        assertThat(result.userId).isNotNull
        assertThat(result.userId).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")

        // then - DB에 저장된 externalId와 일치하는지 검증
        val savedUser = userRepository.findByEmail("yoo-external-id-test@example.com").orElseThrow()
        assertThat(savedUser.id.toString()).isEqualTo(result.userId)
    }
}
