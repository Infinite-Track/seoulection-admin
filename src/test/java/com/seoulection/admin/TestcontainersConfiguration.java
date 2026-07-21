package com.seoulection.admin;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * 모든 {@code @SpringBootTest}가 공유하는 컨테이너 설정.
 *
 * <p><b>왜 클래스마다 {@code @Container}를 두지 않는가:</b> 설문 관리 기능이 들어오면서 앱이 Mongo와
 * Postgres <b>둘 다</b> 없으면 컨텍스트가 아예 안 뜨게 됐다. 컨테이너를 테스트마다 선언하면 하나를 빠뜨린
 * 새 테스트가 "내 코드 문제"처럼 보이는 컨텍스트 로딩 실패로 죽는다. 여기 한 곳에 모아 {@code @Import}만 하면
 * 그 실수가 불가능해지고, 스프링 컨텍스트 캐시도 테스트 클래스 사이에서 공유된다.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    MongoDBContainer mongoContainer() {
        return new MongoDBContainer("mongo:8");
    }

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer("postgres:17");
    }

    /**
     * ⚠️ 운영에서 이 앱의 {@code ddl-auto}는 {@code none}이다(스키마 주인은 api-server).
     * 빈 테스트 컨테이너에는 아무 테이블도 없으므로, 테스트에서만 이 앱의 엔티티 매핑으로 스키마를 만든다.
     *
     * <p>그래서 <b>이 테스트들은 api-server와의 스키마 일치를 증명하지 못한다</b> — 여기서 만드는 건
     * 어디까지나 어드민 쪽 매핑이다. 두 저장소의 컬럼이 어긋나면 통합 실행에서만 드러난다.
     */
    @Bean
    DynamicPropertyRegistrar surveySchemaRegistrar() {
        return registry -> registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }
}
