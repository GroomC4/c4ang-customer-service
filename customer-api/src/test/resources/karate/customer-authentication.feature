Feature: Customer Authentication E2E Scenario
  일반고객 인증 플로우 전체 시나리오 테스트:
  회원가입 → 로그인 → 토큰 리프레시 → 로그아웃

  Background:
    * url baseUrl + apiVersion
    * configure headers = { 'Content-Type': 'application/json', 'Accept': 'application/json' }

    # 테스트용 고유 이메일 생성 (타임스탬프 활용)
    * def timestamp = new Date().getTime()
    * def uniqueEmail = 'testuser' + timestamp + '@example.com'

    # 회원가입 요청 데이터
    * def signupRequest =
    """
    {
      "username": "테스트사용자",
      "email": "#(uniqueEmail)",
      "password": "password123!",
      "defaultAddress": "서울특별시 강남구 테헤란로 123",
      "defaultPhoneNumber": "010-1234-5678"
    }
    """

    # 로그인 요청 데이터
    * def loginRequest =
    """
    {
      "email": "#(uniqueEmail)",
      "password": "password123!"
    }
    """

  Scenario: 일반회원 회원가입 → 로그인 → 토큰 리프레시 → 로그아웃 전체 플로우

    # ===== 1. 회원가입 =====
    Given path '/auth/customers/signup'
    And request signupRequest
    When method post
    Then status 201
    And match response.email == uniqueEmail
    And match response.username == '테스트사용자'
    And match response.role == 'CUSTOMER'
    * print '✅ 회원가입 성공:', response

    # ===== 2. 로그인 =====
    Given path '/auth/customers/login'
    And request loginRequest
    When method post
    Then status 200
    And match response.accessToken == '#notnull'
    And match response.refreshToken == '#notnull'
    And match response.tokenType == 'Bearer'
    And match response.expiresIn == '#number'
    * def accessToken = response.accessToken
    * def refreshToken = response.refreshToken
    * print '✅ 로그인 성공 - Access Token:', accessToken
    * print '✅ 로그인 성공 - Refresh Token:', refreshToken

    # ===== 3. 토큰 리프레시 =====
    # 짧은 대기 (리프레시 토큰이 유효한지 확인)
    * def sleep = function(ms) { java.lang.Thread.sleep(ms) }
    * call sleep 1000

    Given path '/auth/refresh'
    And request { "refreshToken": "#(refreshToken)" }
    When method post
    Then status 200
    And match response.accessToken == '#notnull'
    And match response.tokenType == 'Bearer'
    And match response.expiresIn == '#number'
    * def newAccessToken = response.accessToken
    * print '✅ 토큰 리프레시 성공 - New Access Token:', newAccessToken
    # 주의: 리프레시 시 새로운 Refresh Token은 발급되지 않음 (보안 정책)

    # ===== 4. 로그아웃 (새로 발급받은 Access Token 사용) =====
    Given path '/auth/customers/logout'
    And header Authorization = 'Bearer ' + newAccessToken
    When method post
    Then status 204
    * print '✅ 로그아웃 성공'

    # ===== 5. 로그아웃 후 재인증 API 호출 시도 (실패 예상) =====
    Given path '/auth/customers/logout'
    And header Authorization = 'Bearer ' + newAccessToken
    When method post
    Then status 400
    And match response.code == 'INVALID_REQUEST_PARAMETER'
    And match response.message == '이미 로그아웃된 상태입니다.'
    * print '✅ 로그아웃 후 재호출 시 400 Bad Request 반환 확인 (이미 로그아웃된 상태)'

  Scenario: 잘못된 비밀번호로 로그인 실패 테스트
    # 먼저 회원가입
    Given path '/auth/customers/signup'
    And request signupRequest
    When method post
    Then status 201

    # 잘못된 비밀번호로 로그인 시도
    Given path '/auth/customers/login'
    And request
    """
    {
      "email": "#(uniqueEmail)",
      "password": "wrongpassword123!"
    }
    """
    When method post
    Then status 401
    * print '✅ 잘못된 비밀번호로 로그인 실패 (401) 확인'

  Scenario: 유효하지 않은 리프레시 토큰으로 토큰 갱신 실패 테스트
    Given path '/auth/refresh'
    And request { "refreshToken": "invalid-refresh-token" }
    When method post
    Then status 401
    * print '✅ 유효하지 않은 리프레시 토큰으로 갱신 실패 (401) 확인'
