package com.seoulection.admin.skinpolicy.presentation;

import com.seoulection.admin.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 피부 점수 정책 화면 — 템플릿 오류는 요청을 보내야만 드러난다. 피부타입 규칙 카드가 들어가며 모델이 늘었다. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SkinScorePolicyControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from survey_skin_type_rule");
        jdbcTemplate.update("delete from survey_option");
        jdbcTemplate.update("delete from survey_question");
        jdbcTemplate.update("delete from skin_score_policy");
        jdbcTemplate.update("""
                insert into skin_score_policy (feature_key, label, survey_reliability, photo_reliability,
                    inconsistent_survey_factor, bad_boundary, good_boundary, policy_version, min_answered_weight)
                values ('oily_intensity_score', 'Oiliness', 1, 0, 0.7, 0.25, 0.75, 'v1', 0.5)""");
        jdbcTemplate.update("""
                insert into survey_question (question_key, title, sort_order, active, feature_key, weight, role) values
                    ('OILY_INTENSITY_CORE_V24', 'T-zone', 1, true, 'oily_intensity_score', 50, 'SCORE'),
                    ('OILY_INTENSITY_SITUATIONAL_V24', 'cheeks', 2, true, 'oily_intensity_score', 50, 'SCORE')""");
    }

    @Test
    @DisplayName("정책 화면이 경계 표와 피부타입 규칙 카드(초깃값)를 함께 그린다")
    void page_rendersRuleCard() throws Exception {
        mockMvc.perform(get("/admin/skin-score-policy"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("설문 피부타입 계산 규칙")))
                .andExpect(content().string(containsString("OILY_INTENSITY_CORE_V24 — T-zone")))
                .andExpect(content().string(containsString("name=\"preferSelfDeclared\"")))
                .andExpect(content().string(containsString("최소 답변 비중")));
    }

    @Test
    @DisplayName("규칙 저장은 /skin-type-rule 로 가고 /{featureKey} 와 겹치지 않는다")
    void updateRule_savesAndFlashes() throws Exception {
        mockMvc.perform(post("/admin/skin-score-policy/skin-type-rule")
                        .param("tZoneQuestionKey", "OILY_INTENSITY_CORE_V24")
                        .param("cheekQuestionKey", "OILY_INTENSITY_SITUATIONAL_V24")
                        .param("oilyMax", "25")
                        .param("clearMin", "75")
                        .param("dryWaterMax", "40")
                        .param("preferSelfDeclared", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("successMessage"));

        Boolean preferSelf = jdbcTemplate.queryForObject(
                "select prefer_self_declared from survey_skin_type_rule where id = 1", Boolean.class);
        Integer oilyMax = jdbcTemplate.queryForObject("select oily_max from survey_skin_type_rule where id = 1", Integer.class);
        assertThat(preferSelf).isTrue();
        assertThat(oilyMax).isEqualTo(25);
    }

    @Test
    @DisplayName("뒤집힌 기준은 저장되지 않고 사유가 플래시로 돌아온다")
    void updateRule_invalid_flashesError() throws Exception {
        mockMvc.perform(post("/admin/skin-score-policy/skin-type-rule")
                        .param("tZoneQuestionKey", "OILY_INTENSITY_CORE_V24")
                        .param("cheekQuestionKey", "OILY_INTENSITY_SITUATIONAL_V24")
                        .param("oilyMax", "80")
                        .param("clearMin", "75")
                        .param("dryWaterMax", "40"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("errorMessage"));
        assertThat(jdbcTemplate.queryForObject("select count(*) from survey_skin_type_rule", Long.class)).isZero();
    }
}
