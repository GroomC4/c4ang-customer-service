function fn() {
    // 환경별 설정
    var env = karate.env; // 'dev', 'qa', 'local' 등
    karate.log('karate.env system property was:', env);

    // 기본값은 local
    if (!env) {
        env = 'local';
    }

    // Spring Boot 테스트에서 설정한 랜덤 포트 가져오기
    var port = karate.properties['karate.port'] || '8080';
    karate.log('Using port:', port);

    var config = {
        env: env,
        // 기본 URL 설정 (동적 포트 사용)
        baseUrl: 'http://localhost:' + port,
        apiVersion: '/api/v1',

        // 타임아웃 설정 (밀리초)
        timeout: 30000,

        // 공통 헤더
        headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json'
        }
    };

    // 환경별 설정 오버라이드
    if (env === 'test') {
        // test 환경에서도 동적 포트 사용
        config.baseUrl = 'http://localhost:' + port;
    } else if (env === 'prod') {
        config.baseUrl = 'https://http://ecommerce-alb-749170555.ap-northeast-2.elb.amazonaws.com';
    }

    karate.configure('connectTimeout', config.timeout);
    karate.configure('readTimeout', config.timeout);

    return config;
}
