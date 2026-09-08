package com.seoulection.admin.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * 서비스 호출용 HTTP 클라이언트 기본값.
 *
 * <p>타임아웃을 어댑터가 아니라 여기서 거는 이유: 어댑터가 {@code requestFactory()} 를 부르면
 * 테스트가 붙여 둔 {@code MockRestServiceServer} 의 팩토리를 덮어써서, 흉내 내려던 요청이
 * 진짜로 나가 버린다(2026-09-08 실측). 설정은 설정에 두고 어댑터는 baseUrl 만 정한다.
 *
 * <p>🔴 타임아웃을 빼지 말 것. 기본값은 무한 대기라, 호출 대상이 멈추면 어드민 화면도 함께 멈춘다.
 */
@Configuration
public class AdminHttpClientConfig {

    @Bean
    public RestClient.Builder serviceRestClientBuilder() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        return RestClient.builder().requestFactory(factory);
    }
}
