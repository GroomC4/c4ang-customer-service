Feature: Customer Product List and Search Scenarios
  일반고객의 상품 리스트/검색 시나리오:
  - 전체 상품 리스트 (페이징)
  - 카테고리별 필터링 (카테고리 ID로 필터링 미지원, 검색만 가능)
  - 가격 범위 검색
  - 상품명 키워드 검색
  - 스토어명 검색
  - 정렬 (가격순, 최신순, 평점순)
  - 페이징 기능

  Background:
    * url baseUrl + apiVersion
    * configure headers = { 'Content-Type': 'application/json', 'Accept': 'application/json' }

    # 테스트 데이터 (SQL로 미리 추가된 데이터)
    * def categoryId = '44444444-4444-4444-4444-444444444441'  # 전자제품

    # 테스트용 고유 이메일 생성 (타임스탬프 활용)
    * def timestamp = new Date().getTime()
    * def customerEmail = 'customer_list' + timestamp + '@karate.com'
    * def customerPassword = 'customer123!'

    # 일반고객 회원가입 요청 데이터
    * def signupRequest =
    """
    {
      "username": "리스트테스트고객",
      "email": "#(customerEmail)",
      "password": "#(customerPassword)",
      "phoneNumber": "010-8888-8888",
      "address": "서울시 서초구 테스트로 456"
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


  Scenario: 전체 상품 리스트 조회 - 기본 페이징 (첫 페이지, 10개)

    # ===== 전체 상품 리스트 조회 (기본값) =====
    Given path '/products'
    And header Authorization = 'Bearer ' + accessToken
    When method get
    Then status 200
    * print '✅ 전체 상품 리스트 조회 성공'

    # ===== Response 검증 =====
    # 1. 페이징 정보
    And match response.totalElements == '#notnull'
    And match response.totalPages == '#notnull'
    And match response.currentPage == 0  # 첫 페이지
    And match response.pageSize == 10    # 기본 페이지 크기

    # 2. 상품 목록
    And match response.products == '#notnull'
    And match response.products == '#[]'  # 배열 존재
    * print '전체 상품 개수:', response.totalElements
    * print '전체 페이지 수:', response.totalPages
    * print '현재 페이지 상품 개수:', response.products.length

    # 3. 첫 번째 상품 구조 검증 (상품이 있는 경우)
    * if (response.products.length > 0) karate.call('classpath:karate-helpers/validate-product-summary.js', response.products[0])

    * print '✅ 전체 상품 리스트 페이징 정보 검증 완료'


  Scenario: 페이징 기능 - 두 번째 페이지 조회

    # ===== 두 번째 페이지 조회 (page=1, size=5) =====
    Given path '/products'
    And header Authorization = 'Bearer ' + accessToken
    And param page = 1
    And param size = 5
    When method get
    Then status 200
    * print '✅ 두 번째 페이지 조회 성공'

    # ===== Response 검증 =====
    And match response.currentPage == 1
    And match response.pageSize == 5
    And match response.products == '#[_]'  # 최대 5개
    * print '두 번째 페이지 상품 개수:', response.products.length

    * print '✅ 페이징 기능 검증 완료'


  Scenario: 가격 범위 검색 - 10,000원 ~ 50,000원

    # ===== 가격 범위 검색 =====
    Given path '/products'
    And header Authorization = 'Bearer ' + accessToken
    And param minPrice = 10000
    And param maxPrice = 50000
    When method get
    Then status 200
    * print '✅ 가격 범위 검색 성공'

    # ===== Response 검증 =====
    And match response.products == '#[]'

    # 모든 상품의 가격이 범위 내에 있는지 검증
    * def allPricesInRange = function(products) { for (var i = 0; i < products.length; i++) { var price = products[i].price; if (price < 10000 || price > 50000) return false; } return true; }
    * assert allPricesInRange(response.products)

    * print '✅ 가격 범위 내 상품만 조회됨'
    * print '조회된 상품 개수:', response.products.length


  Scenario: 상품명 키워드 검색 - "마우스"

    # ===== 상품명 키워드 검색 =====
    Given path '/products'
    And header Authorization = 'Bearer ' + accessToken
    And param productName = '마우스'
    When method get
    Then status 200
    * print '✅ 상품명 키워드 검색 성공'

    # ===== Response 검증 =====
    And match response.products == '#[]'

    # 조회된 상품명에 "마우스"가 포함되어 있는지 검증
    * def allNamesContainKeyword = function(products, keyword) { for (var i = 0; i < products.length; i++) { if (products[i].name.indexOf(keyword) === -1) return false; } return true; }
    * assert allNamesContainKeyword(response.products, '마우스')

    * print '✅ "마우스" 키워드가 포함된 상품만 조회됨'
    * print '조회된 상품 개수:', response.products.length


  Scenario: 스토어명 검색 - "테크노 전자"

    # ===== 스토어명 검색 =====
    Given path '/products'
    And header Authorization = 'Bearer ' + accessToken
    And param storeName = '테크노 전자'
    When method get
    Then status 200
    * print '✅ 스토어명 검색 성공'

    # ===== Response 검증 =====
    And match response.products == '#[]'

    # 조회된 상품의 스토어명이 "테크노 전자"인지 검증
    * def allStoresMatch = function(products, storeName) { for (var i = 0; i < products.length; i++) { if (products[i].storeName !== storeName) return false; } return true; }
    * assert allStoresMatch(response.products, '테크노 전자')

    * print '✅ "테크노 전자" 스토어의 상품만 조회됨'
    * print '조회된 상품 개수:', response.products.length


  Scenario: 정렬 기능 - 가격 낮은 순 (ASC)

    # ===== 가격 낮은 순 정렬 =====
    Given path '/products'
    And header Authorization = 'Bearer ' + accessToken
    And param sortBy = 'price'
    And param sortOrder = 'asc'
    And param size = 5
    When method get
    Then status 200
    * print '✅ 가격 낮은 순 정렬 조회 성공'

    # ===== Response 검증 =====
    And match response.products == '#[_]'

    # 가격이 오름차순으로 정렬되어 있는지 검증
    * def isSortedAsc = function(products) { for (var i = 1; i < products.length; i++) { if (products[i-1].price > products[i].price) return false; } return true; }
    * assert isSortedAsc(response.products)

    * print '✅ 가격 오름차순 정렬 확인'
    * print '첫 번째 상품 가격:', response.products[0].price
    * if (response.products.length > 1) print('마지막 상품 가격:', response.products[response.products.length - 1].price)


  Scenario: 정렬 기능 - 가격 높은 순 (DESC)

    # ===== 가격 높은 순 정렬 =====
    Given path '/products'
    And header Authorization = 'Bearer ' + accessToken
    And param sortBy = 'price'
    And param sortOrder = 'desc'
    And param size = 5
    When method get
    Then status 200
    * print '✅ 가격 높은 순 정렬 조회 성공'

    # ===== Response 검증 =====
    And match response.products == '#[_]'

    # 가격이 내림차순으로 정렬되어 있는지 검증
    * def isSortedDesc = function(products) { for (var i = 1; i < products.length; i++) { if (products[i-1].price < products[i].price) return false; } return true; }
    * assert isSortedDesc(response.products)

    * print '✅ 가격 내림차순 정렬 확인'
    * print '첫 번째 상품 가격:', response.products[0].price
    * if (response.products.length > 1) print('마지막 상품 가격:', response.products[response.products.length - 1].price)


  Scenario: 정렬 기능 - 최신순 (created_at DESC)

    # ===== 최신순 정렬 =====
    Given path '/products'
    And header Authorization = 'Bearer ' + accessToken
    And param sortBy = 'created_at'
    And param sortOrder = 'desc'
    And param size = 5
    When method get
    Then status 200
    * print '✅ 최신순 정렬 조회 성공'

    # ===== Response 검증 =====
    And match response.products == '#[_]'

    * print '✅ 최신순 정렬 조회 완료 (상품 날짜 비교는 생략)'
    * print '조회된 상품 개수:', response.products.length


  Scenario: 정렬 기능 - 평점 높은 순 (rating DESC)

    # ===== 평점 높은 순 정렬 =====
    Given path '/products'
    And header Authorization = 'Bearer ' + accessToken
    And param sortBy = 'rating'
    And param sortOrder = 'desc'
    And param size = 5
    When method get
    Then status 200
    * print '✅ 평점 높은 순 정렬 조회 성공'

    # ===== Response 검증 =====
    And match response.products == '#[_]'

    # 평점이 내림차순으로 정렬되어 있는지 검증 (null 체크 포함)
    * def isSortedByRatingDesc = function(products) { for (var i = 1; i < products.length; i++) { var prev = products[i-1].averageRating || 0; var curr = products[i].averageRating || 0; if (prev < curr) return false; } return true; }
    * assert isSortedByRatingDesc(response.products)

    * print '✅ 평점 내림차순 정렬 확인'
    * print '첫 번째 상품 평점:', response.products[0].averageRating
    * if (response.products.length > 1) print('마지막 상품 평점:', response.products[response.products.length - 1].averageRating)


  Scenario: 복합 검색 - 가격 범위 + 상품명 키워드 + 정렬

    # ===== 복합 검색: 10,000~100,000원 범위, "키보드" 키워드, 가격 낮은 순 =====
    Given path '/products'
    And header Authorization = 'Bearer ' + accessToken
    And param minPrice = 10000
    And param maxPrice = 100000
    And param productName = '키보드'
    And param sortBy = 'price'
    And param sortOrder = 'asc'
    When method get
    Then status 200
    * print '✅ 복합 검색 성공'

    # ===== Response 검증 =====
    And match response.products == '#[]'

    # 조건 검증 (상품이 있는 경우)
    * if (response.products.length > 0) {
        var allPricesInRange = true;
        for (var i = 0; i < response.products.length; i++) {
          var price = response.products[i].price;
          if (price < 10000 || price > 100000) {
            allPricesInRange = false;
            break;
          }
        }
        karate.assert(allPricesInRange, '모든 상품이 가격 범위 내에 있어야 함');

        var allNamesContainKeyword = true;
        for (var i = 0; i < response.products.length; i++) {
          if (response.products[i].name.indexOf('키보드') === -1) {
            allNamesContainKeyword = false;
            break;
          }
        }
        karate.assert(allNamesContainKeyword, '모든 상품명에 "키보드" 키워드가 포함되어야 함');

        var isSortedAsc = true;
        for (var i = 1; i < response.products.length; i++) {
          if (response.products[i-1].price > response.products[i].price) {
            isSortedAsc = false;
            break;
          }
        }
        karate.assert(isSortedAsc, '가격이 오름차순으로 정렬되어야 함');
      }

    * print '✅ 복합 검색 조건 모두 만족'
    * print '조회된 상품 개수:', response.products.length


  Scenario: 유사도 검색 - "무선 마우쓰" 오타 허용

    # ===== 유사도 검색 (pg_trgm 활용) =====
    Given path '/products'
    And header Authorization = 'Bearer ' + accessToken
    And param productName = '무선 마우쓰'
    And param useSimilaritySearch = true
    When method get
    Then status 200
    * print '✅ 유사도 검색 성공'

    # ===== Response 검증 =====
    And match response.products == '#[]'

    * print '✅ 유사도 검색 결과 조회 (오타 허용)'
    * print '조회된 상품 개수:', response.products.length
    * if (response.products.length > 0) print('첫 번째 상품명:', response.products[0].name)
