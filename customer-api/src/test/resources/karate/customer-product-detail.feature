Feature: Customer Product Detail View Scenarios
  일반고객의 상품 상세 조회 시나리오:
  - 정상 조회 (이미지, 카테고리, 스토어 정보 포함)
  - 존재하지 않는 상품 조회 (404)
  - 삭제된 상품 조회 (404)
  - 숨겨진 상품 조회 (현재는 200, 나중에 404로 변경 예정)

  Background:
    * url baseUrl + apiVersion
    * configure headers = { 'Content-Type': 'application/json', 'Accept': 'application/json' }

    # 테스트 데이터 (SQL로 미리 추가된 데이터)
    * def validProductId = '55555555-5555-5555-5555-555555555501'
    * def deletedProductId = '55555555-5555-5555-5555-555555555510'
    * def hiddenProductId = '55555555-5555-5555-5555-555555555520'
    * def nonExistentProductId = '00000000-0000-0000-0000-000000000000'

    # 테스트용 고유 이메일 생성 (타임스탬프 활용)
    * def timestamp = new Date().getTime()
    * def customerEmail = 'customer' + timestamp + '@karate.com'
    * def customerPassword = 'customer123!'

    # 일반고객 회원가입 요청 데이터
    * def signupRequest =
    """
    {
      "username": "테스트고객",
      "email": "#(customerEmail)",
      "password": "#(customerPassword)",
      "phoneNumber": "010-9999-9999",
      "address": "서울시 강남구 테스트로 123"
    }
    """

    # 로그인 요청 데이터
    * def loginRequest =
    """
    {
      "email": "#(customerEmail)",
      "password": "#(customerPassword)"
    }
    """

    # 일반고객 회원가입
    Given path '/auth/customers/signup'
    And request signupRequest
    When method post
    Then status 201
    * print '✅ 일반고객 회원가입 성공'

    # 일반고객 로그인
    Given path '/auth/customers/login'
    And request loginRequest
    When method post
    Then status 200
    And match response.accessToken == '#notnull'
    * def accessToken = response.accessToken
    * print '✅ 일반고객 로그인 성공 - Access Token:', accessToken


  Scenario: 정상 상품 상세 조회 - 이미지, 카테고리, 스토어 정보 포함

    # ===== 상품 상세 조회 =====
    Given path '/products/' + validProductId
    And header Authorization = 'Bearer ' + accessToken
    When method get
    Then status 200
    * print '✅ 상품 상세 조회 성공'

    # ===== Response 검증 =====
    # 1. 상품 기본 정보
    And match response.id == validProductId
    And match response.name == '무선 마우스 Pro'
    And match response.description == '편안한 그립감의 무선 마우스'
    And match response.price == 29900.00
    And match response.stockQuantity == 150
    And match response.status == 'ON_SALE'

    # 2. 카테고리 정보
    And match response.categoryId == '44444444-4444-4444-4444-44444444441b'
    And match response.categoryPath == '#notnull'  # 카테고리 경로 존재 확인
    * print '카테고리 경로:', response.categoryPath

    # 3. 스토어 정보
    And match response.store == '#notnull'
    And match response.store.id == '33333333-3333-3333-3333-333333333331'
    And match response.store.name == '테크노 전자'

    # 4. 상품 이미지 목록
    And match response.images == '#notnull'
    And match response.images == '#[1]'  # 1개의 이미지
    And match response.images[0].imageUrl == 'https://example.com/images/mouse_pro_primary.jpg'
    And match response.images[0].imageType == 'PRIMARY'

    # 5. 평점 및 리뷰 정보 (테스트 데이터에는 리뷰가 없으므로 null 또는 0)
    # Note: 실제 프로덕션에서는 리뷰 시스템과 연동되어 평점 데이터가 존재할 수 있음
    And match response.rating == '#present'
    And match response.reviewCount == '#present'

    # 6. 생성일시
    And match response.createdAt == '#notnull'

    * print '✅ 상품 상세 정보 검증 완료 (이미지, 카테고리, 스토어 정보 포함)'


  Scenario: 존재하지 않는 상품 조회 - 404 에러

    # ===== 존재하지 않는 상품 조회 시도 =====
    Given path '/products/' + nonExistentProductId
    And header Authorization = 'Bearer ' + accessToken
    When method get
    Then status 404
    * print '✅ 존재하지 않는 상품 조회 시 404 반환 확인'

    # ===== 에러 메시지 검증 (선택) =====
    And match response.message == '#notnull'
    * print '에러 메시지:', response.message


  Scenario: 삭제된 상품 조회 - 404 에러

    # ===== 삭제된 상품 조회 시도 =====
    Given path '/products/' + deletedProductId
    And header Authorization = 'Bearer ' + accessToken
    When method get
    Then status 404
    * print '✅ 삭제된 상품 조회 시 404 반환 확인'

    # ===== 에러 메시지 검증 (선택) =====
    And match response.message == '#notnull'
    * print '에러 메시지:', response.message


  Scenario: 숨겨진 상품 조회 - 현재는 200 (나중에 404로 변경 예정)

    # ===== 숨겨진 상품 조회 시도 =====
    # 참고: 현재 구현에서는 숨긴 상품도 조회 가능 (hiddenAt 체크 없음)
    # TODO: 추후 숨긴 상품 조회 시 404 반환하도록 변경 필요
    Given path '/products/' + hiddenProductId
    And header Authorization = 'Bearer ' + accessToken
    When method get
    Then status 200
    * print '✅ 숨겨진 상품 조회 (현재는 조회 가능 - 200 반환)'

    # ===== Response 검증 (숨긴 상품도 정상 조회됨) =====
    And match response.id == hiddenProductId
    And match response.name == '숨겨진 상품'
    * print '⚠️  숨긴 상품이 조회됨 - 향후 404로 변경 예정'
