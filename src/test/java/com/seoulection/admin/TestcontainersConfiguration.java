package com.seoulection.admin;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.InitializingBean;
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

    /** JdbcTemplate으로 관리하는 성분 카탈로그는 Hibernate가 DDL을 만들 수 없으므로 테스트에서만 준비한다. */
    @Bean
    InitializingBean ingredientSchemaRegistrar(JdbcTemplate jdbc) {
        return () -> {
            jdbc.execute("create table if not exists ingredient (id varchar(64) primary key, inci_name varchar(200) not null, display_name_ko varchar(100) not null, family varchar(80) not null)");
            jdbc.execute("create table if not exists ingredient_alias (id bigserial primary key, ingredient_id varchar(64) not null, alias varchar(200) not null, alias_type varchar(40) not null, unique (ingredient_id, alias))");
            jdbc.execute("create table if not exists ingredient_effect (id bigserial primary key, ingredient_id varchar(64) not null, target_key varchar(80) not null, role varchar(20) not null, unique (ingredient_id, target_key))");
            jdbc.execute("create table if not exists property_definition (property_key varchar(80) primary key, display_name_ko varchar(100) not null, value_type varchar(30) not null, value_unit varchar(30), description varchar(500))");
            jdbc.update("insert into property_definition(property_key, display_name_ko, value_type) values ('SOLUBILITY', '용해성', 'TEXT'), ('STABILITY', '안정성', 'TEXT') on conflict (property_key) do nothing");
            jdbc.execute("create table if not exists ingredient_property (id bigserial primary key, ingredient_id varchar(64) not null, property_key varchar(80) not null, value_text text, value_min numeric(14,5), value_max numeric(14,5), value_unit varchar(30), source_id bigint, confidence numeric(5,4), notes text, unique (ingredient_id, property_key))");
            jdbc.execute("create table if not exists evidence_source (id bigserial primary key, source_type varchar(40) not null, title varchar(500) not null, url varchar(1000), publisher varchar(200), published_at date, accessed_at date, citation text)");
            jdbc.execute("create table if not exists ingredient_evidence (id bigserial primary key, ingredient_id varchar(64) not null, source_id bigint not null, human_evidence boolean not null default false, evidence_level varchar(30) not null, target_score varchar(80), ingredient_specific boolean not null default false, product_form varchar(80), study_concentration numeric(14,5), study_concentration_unit varchar(30), summary text, unique (ingredient_id, source_id))");
            jdbc.execute("create table if not exists ingredient_efficacy_range (id bigserial primary key, ingredient_id varchar(64) not null, target_key varchar(80) not null, product_type varchar(80), concentration_min numeric(14,5), concentration_max numeric(14,5), concentration_unit varchar(30), onset_concentration numeric(14,5), irritation_concentration numeric(14,5), evidence_id bigint, notes text)");
            jdbc.execute("create table if not exists product_ingredient (id bigserial primary key, product_id varchar(64) not null, ingredient_id varchar(64), raw_name varchar(300) not null, inci_order integer, concentration_min numeric(14,5), concentration_max numeric(14,5), concentration_unit varchar(30), source varchar(40) not null, unique (product_id, inci_order, raw_name))");
            jdbc.execute("create table if not exists product_ingredient_property (id bigserial primary key, product_ingredient_id bigint not null, property_key varchar(80) not null, value_text text, value_min numeric(14,5), value_max numeric(14,5), value_unit varchar(30), source varchar(40) not null, confidence numeric(5,4), notes text, unique (product_ingredient_id, property_key))");
        };
    }
}
