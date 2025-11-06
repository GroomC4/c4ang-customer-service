Feature: Owner Authentication E2E Scenario
  판매자 인증 플로우 전체 시나리오 테스트:
  회원가입 → 로그인 → 토큰 리프레시 → 로그아웃

  Background:
    * url baseUrl + apiVersion
    * configure headers = { 'Content-Type': 'application/json', 'Accept': 'application/json' }

    # 테스트용 고유 이메일 및 스토어명 생성 (타임스탬프 활용)
    * def timestamp = new Date().getTime()
    * def uniqueEmail = 'testowner' + timestamp + '@example.com'
    * def uniqueStoreName = '테스트스토어' + timestamp

    # 회원가입 요청 데이터
    * def signupRequest =
    """
    {
      "username": "테스트판매자",
      "email": "#(uniqueEmail)",
      "password": "ownerpass123!",
      "phoneNumber": "010-9876-5432",
      "storeInfo": {
        "name": "#(uniqueStoreName)",
        "description": "최고의 상품을 판매하는 테스트 스토어입니다"
      }
    }
    """

    # 로그인 요청 데이터
    * def loginRequest =
    """
    {
      "email": "#(uniqueEmail)",
      "password": "ownerpass123!"
    }
    """

  Scenario: 판매자 회원가입 → 로그인 → 토큰 리프레시 → 로그아웃 전체 플로우

    # ===== 1. 판매자 회원가입 =====
    Given path '/auth/owners/signup'
    And request signupRequest
    When method post
    Then status 201
    And match response.user.email == uniqueEmail
    And match response.user.name == '테스트판매자'
    And match response.user.id == '#notnull'
    And match response.store.id == '#notnull'
    And match response.store.name == uniqueStoreName
    And match response.createdAt == '#notnull'
    * def userId = response.user.id
    * def storeId = response.store.id
    * print '✅ 판매자 회원가입 성공:', response
    * print '✅ 생성된 사용자 ID:', userId
    * print '✅ 생성된 스토어 ID:', storeId

    # ===== 2. 판매자 로그인 =====
    Given path '/auth/owners/login'
    And request loginRequest
    When method post
    Then status 200
    And match response.accessToken == '#notnull'
    And match response.refreshToken == '#notnull'
    And match response.tokenType == 'Bearer'
    And match response.expiresIn == '#number'
    * def accessToken = response.accessToken
    * def refreshToken = response.refreshToken
    * print '✅ 판매자 로그인 성공 - Access Token:', accessToken
    * print '✅ 판매자 로그인 성공 - Refresh Token:', refreshToken

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
    Given path '/auth/owners/logout'
    And header Authorization = 'Bearer ' + newAccessToken
    When method post
    Then status 204
    * print '✅ 판매자 로그아웃 성공'

    # ===== 5. 로그아웃 후 재인증 API 호출 시도 (실패 예상) =====
    Given path '/auth/owners/logout'
    And header Authorization = 'Bearer ' + newAccessToken
    When method post
    Then status 400
    And match response.code == 'INVALID_REQUEST_PARAMETER'
    And match response.message == '이미 로그아웃된 상태입니다.'
    * print '✅ 로그아웃 후 재호출 시 400 Bad Request 반환 확인 (이미 로그아웃된 상태)'

  Scenario: 잘못된 비밀번호로 판매자 로그인 실패 테스트
    # 먼저 회원가입
    Given path '/auth/owners/signup'
    And request signupRequest
    When method post
    Then status 201

    # 잘못된 비밀번호로 로그인 시도
    Given path '/auth/owners/login'
    And request
    """
    {
      "email": "#(uniqueEmail)",
      "password": "wrongpassword123!"
    }
    """
    When method post
    Then status 401
    * print '✅ 잘못된 비밀번호로 판매자 로그인 실패 (401) 확인'

  Scenario: 중복된 이메일로 판매자 회원가입 실패 테스트
    # 첫 번째 회원가입 성공
    Given path '/auth/owners/signup'
    And request signupRequest
    When method post
    Then status 201

    # 동일한 이메일로 재가입 시도 (스토어명은 다르게)
    * def duplicateEmailRequest =
    """
    {
      "username": "중복테스트판매자",
      "email": "#(uniqueEmail)",
      "password": "anotherpass123!",
      "phoneNumber": "010-1111-2222",
      "storeInfo": {
        "name": "다른스토어이름",
        "description": "다른 스토어입니다"
      }
    }
    """

    Given path '/auth/owners/signup'
    And request duplicateEmailRequest
    When method post
    Then status 409
    And match response.code == 'DUPLICATE_EMAIL'
    * print '✅ 중복된 이메일로 판매자 회원가입 실패 (409) 확인'

  Scenario: 유효하지 않은 이메일 형식으로 판매자 회원가입 실패 테스트
    * def invalidEmailRequest =
    """
    {
      "username": "테스트판매자",
      "email": "invalid-email-format",
      "password": "ownerpass123!",
      "phoneNumber": "010-9876-5432",
      "storeInfo": {
        "name": "테스트스토어",
        "description": "테스트 스토어입니다"
      }
    }
    """

    Given path '/auth/owners/signup'
    And request invalidEmailRequest
    When method post
    Then status 400
    * print '✅ 유효하지 않은 이메일 형식으로 판매자 회원가입 실패 (400) 확인'
