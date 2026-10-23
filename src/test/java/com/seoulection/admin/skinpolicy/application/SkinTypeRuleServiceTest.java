package com.seoulection.admin.skinpolicy.application;

import com.seoulection.admin.TestcontainersConfiguration;
import com.seoulection.admin.skinpolicy.domain.SkinTypeRule;
import com.seoulection.admin.survey.domain.entity.SurveyQuestion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 피부타입 계산 규칙(한 행) — 실제 Postgres 로 왕복한다. 스키마는 어드민 매핑으로 만든 테스트용이다. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class SkinTypeRuleServiceTest {

    @Autowired
    SkinTypeRuleService service;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from survey_skin_type_rule");
        jdbcTemplate.update("delete from survey_option");
        jdbcTemplate.update("delete from survey_question");
        jdbcTemplate.update("""
                insert into survey_question (question_key, title, sort_order, active, feature_key, weight, role) values
                    ('OILY_INTENSITY_CORE_V24', 'T-zone', 1, true, 'oily_intensity_score', 50, 'SCORE'),
                    ('OILY_INTENSITY_SITUATIONAL_V24', 'cheeks', 2, true, 'oily_intensity_score', 50, 'SCORE'),
                    ('OILY_INTENSITY_DIRECT', 'old', 3, false, 'oily_intensity_score', 50, 'SCORE'),
                    ('WATER_CORE_V24', 'water', 4, true, 'water_score', 50, 'SCORE')""");
    }

    @Test
    @DisplayName("행이 없으면 V34 초깃값을 보여주고, 저장하면 그때 행이 생긴다")
    void get_withoutRow_returnsDefault_thenSaveCreatesRow() {
        assertThat(service.get()).isEqualTo(SkinTypeRule.DEFAULT);

        service.update("OILY_INTENSITY_CORE_V24", "OILY_INTENSITY_SITUATIONAL_V24", 25, 75, 40, true);

        SkinTypeRule saved = service.get();
        assertThat(saved.oilyMax()).isEqualTo(25);
        assertThat(saved.preferSelfDeclared()).isTrue();
        Long rows = jdbcTemplate.queryForObject("select count(*) from survey_skin_type_rule", Long.class);
        assertThat(rows).isEqualTo(1L);

        // 다시 저장해도 행은 하나(id = 1 upsert).
        service.update("OILY_INTENSITY_CORE_V24", "OILY_INTENSITY_SITUATIONAL_V24", 50, 75, 40, false);
        assertThat(jdbcTemplate.queryForObject("select count(*) from survey_skin_type_rule", Long.class)).isEqualTo(1L);
        assertThat(service.get().preferSelfDeclared()).isFalse();
    }

    @Test
    @DisplayName("T존·볼 후보는 유분 지표에 묶인 활성 문항뿐 — 숨긴 옛 문항과 다른 지표 문항은 빠진다")
    void oilQuestionCandidates_onlyActiveOilQuestions() {
        assertThat(service.oilQuestionCandidates()).extracting(SurveyQuestion::getQuestionKey)
                .containsExactly("OILY_INTENSITY_CORE_V24", "OILY_INTENSITY_SITUATIONAL_V24");
    }

    @Test
    @DisplayName("없는 문항·같은 문항·뒤집힌 기준은 저장 전에 거부된다")
    void update_rejectsInvalid() {
        assertThatThrownBy(() -> service.update("NOPE", "OILY_INTENSITY_SITUATIONAL_V24", 50, 75, 40, false))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("없는 문항");
        assertThatThrownBy(() -> service.update("OILY_INTENSITY_CORE_V24", "OILY_INTENSITY_CORE_V24", 50, 75, 40, false))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("서로 달라야");
        assertThatThrownBy(() -> service.update("OILY_INTENSITY_CORE_V24", "OILY_INTENSITY_SITUATIONAL_V24", 75, 50, 40, false))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("작아야");
    }
}
