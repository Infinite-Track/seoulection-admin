package com.seoulection.admin.survey.application;

import com.seoulection.admin.TestcontainersConfiguration;
import com.seoulection.admin.survey.application.dto.SurveyOptionResult;
import com.seoulection.admin.survey.application.dto.SurveyQuestionResult;
import com.seoulection.admin.survey.application.service.SurveyAdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 설문 관리 CRUD — 실제 Postgres로 왕복을 확인한다.
 *
 * <p>여기서 만드는 스키마는 <b>어드민 쪽 매핑</b>이다(운영에선 api-server가 소유).
 * 그래서 이 테스트는 "어드민 로직이 맞는가"를 보증하지, 두 저장소의 컬럼 일치는 보증하지 않는다.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class SurveyAdminServiceTest {

    @Autowired
    SurveyAdminService service;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from survey_option");
        jdbcTemplate.update("delete from survey_question");
        jdbcTemplate.update("""
                insert into survey_question (question_key, title, sort_order)
                values ('AVOIDANCE', '회피 항목', 1), ('CONCERN', '피부 고민', 2)""");
    }

    private List<SurveyOptionResult> avoidanceOptions() {
        return service.getQuestions().stream()
                .filter(q -> q.key().equals("AVOIDANCE"))
                .findFirst().orElseThrow()
                .options();
    }

    @Test
    @DisplayName("선택지를 추가하면 활성 상태로 저장되고 코드는 대문자로 정규화된다")
    void createOption_savesActiveWithUppercasedCode() {
        service.createOption("AVOIDANCE", "fragrance_allergy", "향료 회피", null, 3, false);

        SurveyOptionResult saved = avoidanceOptions().get(0);
        assertThat(saved.code()).isEqualTo("FRAGRANCE_ALLERGY");
        assertThat(saved.label()).isEqualTo("향료 회피");
        assertThat(saved.sortOrder()).isEqualTo(3);
        assertThat(saved.active()).isTrue();
    }

    @Test
    @DisplayName("점수가 있는 선택지(자가진단 문항 등)는 value가 그대로 저장·수정된다")
    void createOption_withValue_savesScore() {
        service.createOption("AVOIDANCE", "WATER_BALANCED", "적당하다", 66, 1, true);

        SurveyOptionResult saved = avoidanceOptions().get(0);
        assertThat(saved.value()).isEqualTo(66);

        service.updateOption(saved.id(), saved.label(), 100, saved.sortOrder(), saved.exclusive());
        assertThat(avoidanceOptions().get(0).value()).isEqualTo(100);
    }

    @Test
    @DisplayName("같은 문항에 같은 코드를 또 추가하면 거부된다 — 응답이 어느 선택지를 가리키는지 모호해지므로")
    void createOption_duplicateCode_rejected() {
        service.createOption("AVOIDANCE", "PREGNANT", "임신 중", null, 1, false);

        assertThatThrownBy(() ->
                service.createOption("AVOIDANCE", "PREGNANT", "다른 문구", null, 2, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 있는 코드");
    }

    @Test
    @DisplayName("소문자·공백이 섞인 코드 형식은 거부된다")
    void createOption_invalidCodeFormat_rejected() {
        assertThatThrownBy(() ->
                service.createOption("AVOIDANCE", "fragrance allergy", "향료", null, 1, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("★ 수정은 문구·점수·순서·단독선택만 바꾸고 code는 그대로다 — 기존 응답이 가리키는 키가 유지된다")
    void updateOption_keepsCode() {
        service.createOption("AVOIDANCE", "PREGNANT", "임신 중", null, 1, false);
        Long id = avoidanceOptions().get(0).id();

        service.updateOption(id, "임신 중이에요", null, 9, true);

        SurveyOptionResult updated = avoidanceOptions().get(0);
        assertThat(updated.code()).isEqualTo("PREGNANT");
        assertThat(updated.label()).isEqualTo("임신 중이에요");
        assertThat(updated.sortOrder()).isEqualTo(9);
        assertThat(updated.exclusive()).isTrue();
    }

    @Test
    @DisplayName("★ 숨김은 행을 지우지 않는다(soft delete) — 되살릴 수 있고 과거 응답의 문구도 남는다")
    void changeOptionActive_isSoftDelete() {
        service.createOption("AVOIDANCE", "PREGNANT", "임신 중", null, 1, false);
        Long id = avoidanceOptions().get(0).id();

        service.changeOptionActive(id, false);

        assertThat(avoidanceOptions()).hasSize(1);
        assertThat(avoidanceOptions().get(0).active()).isFalse();
        Long rows = jdbcTemplate.queryForObject(
                "select count(*) from survey_option where code = 'PREGNANT'", Long.class);
        assertThat(rows).isEqualTo(1L);

        service.changeOptionActive(id, true);
        assertThat(avoidanceOptions().get(0).active()).isTrue();
    }

    @Test
    @DisplayName("선택지는 sortOrder 순으로 조회된다 — 관리 화면 순서가 곧 사용자 화면 순서다")
    void getQuestions_ordersBySortOrder() {
        service.createOption("AVOIDANCE", "SECOND", "둘째", null, 2, false);
        service.createOption("AVOIDANCE", "FIRST", "첫째", null, 1, false);

        assertThat(avoidanceOptions().stream().map(SurveyOptionResult::code))
                .containsExactly("FIRST", "SECOND");
    }

    @Test
    @DisplayName("질문 문구 수정은 새 행을 만들지 않는다(자연 PK upsert)")
    void updateQuestionTitle_upsertsInPlace() {
        service.updateQuestionTitle("AVOIDANCE", "바뀐 질문 문구");

        List<SurveyQuestionResult> questions = service.getQuestions();
        assertThat(questions).hasSize(2);
        assertThat(questions.get(0).title()).isEqualTo("바뀐 질문 문구");
    }
}
