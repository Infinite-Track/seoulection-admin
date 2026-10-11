package com.seoulection.admin.survey.presentation;

import com.seoulection.admin.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 설문 관리 화면이 실제로 그려지는지 — 템플릿 오류는 컴파일로 못 잡고 요청을 보내야만 드러난다.
 * 지표 매핑(V30) 폼이 들어가면서 템플릿이 features/roles 모델을 요구하게 됐으므로, 정상 렌더와
 * 검증 실패 재렌더 둘 다 확인한다(컨트롤러 주석의 "return survey 가 여러 군데" 함정).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SurveyAdminControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from survey_option");
        jdbcTemplate.update("delete from survey_question");
        jdbcTemplate.update("delete from skin_score_policy");
        jdbcTemplate.update("""
                insert into skin_score_policy (feature_key, label, survey_reliability, photo_reliability,
                    inconsistent_survey_factor, bad_boundary, good_boundary, policy_version, min_answered_weight)
                values ('water_score', 'Hydration', 1, 0, 0.7, 0.33, 1, 'v1', 0.5)""");
        jdbcTemplate.update("""
                insert into survey_question (question_key, title, sort_order, active, feature_key, weight, role)
                values ('WATER_DIRECT', '평소 건조한 정도', 1, true, 'water_score', 50, 'SCORE'),
                       ('AVOIDANCE', '회피 항목', 2, true, null, 100, 'FLAG')""");
        jdbcTemplate.update("""
                insert into survey_option (question_key, code, label, value, sort_order, exclusive, active,
                    caps_feature_at, refer_dermatologist)
                values ('WATER_DIRECT', 'VERY_DRY', '매우 건조', 0, 1, false, true, null, false)""");
    }

    @Test
    @DisplayName("설문 관리 화면이 지표 매핑 폼·지표별 비중 요약과 함께 그려진다")
    void page_rendersMappingControls() throws Exception {
        mockMvc.perform(get("/admin/survey"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("features", "roles", "questions"))
                .andExpect(content().string(containsString("지표별 비중 합")))
                .andExpect(content().string(containsString("Hydration (water_score)")))
                .andExpect(content().string(containsString("점수 → Hydration")))
                .andExpect(content().string(containsString("점수에 안 쓰임")))
                .andExpect(content().string(containsString("name=\"capsFeatureAt\"")))
                .andExpect(content().string(containsString("name=\"referDermatologist\"")));
    }

    @Test
    @DisplayName("문항 추가 폼에서 지표·비중·역할을 함께 보내면 저장되고 목록으로 돌아간다")
    void createQuestion_withMapping_redirects() throws Exception {
        mockMvc.perform(post("/admin/survey/questions")
                        .param("questionKey", "WATER_SITUATIONAL")
                        .param("title", "지난 7일 건조해 보인 날")
                        .param("sortOrder", "3")
                        .param("featureKey", "water_score")
                        .param("weight", "50")
                        .param("role", "SCORE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/survey"));

        Integer weight = jdbcTemplate.queryForObject(
                "select weight from survey_question where question_key = 'WATER_SITUATIONAL'", Integer.class);
        org.assertj.core.api.Assertions.assertThat(weight).isEqualTo(50);
    }

    @Test
    @DisplayName("정책에 없는 지표로 문항을 추가하면 폼이 다시 열리고 사유가 보인다(500 이 아니다)")
    void createQuestion_unknownFeature_rerendersForm() throws Exception {
        mockMvc.perform(post("/admin/survey/questions")
                        .param("questionKey", "WATER_X")
                        .param("title", "문구")
                        .param("sortOrder", "3")
                        .param("featureKey", "hydration")
                        .param("weight", "50")
                        .param("role", "SCORE"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("questionFormOpen", true))
                .andExpect(content().string(containsString("없는 피부 지표입니다")));
    }

    @Test
    @DisplayName("문항 카드의 매핑 저장은 지표·비중·역할을 바꾸고 안내 문구를 띄운다")
    void updateMapping_savesAndFlashes() throws Exception {
        mockMvc.perform(post("/admin/survey/questions/WATER_DIRECT/mapping")
                        .param("featureKey", "water_score")
                        .param("weight", "70")
                        .param("role", "MODIFIER"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("successMessage"));

        String role = jdbcTemplate.queryForObject(
                "select role from survey_question where question_key = 'WATER_DIRECT'", String.class);
        org.assertj.core.api.Assertions.assertThat(role).isEqualTo("MODIFIER");
    }

    @Test
    @DisplayName("선택지 행 저장에 지표 상한·진료 안내가 실린다")
    void updateOption_savesCapAndReferral() throws Exception {
        Long id = jdbcTemplate.queryForObject("select id from survey_option where code = 'VERY_DRY'", Long.class);

        mockMvc.perform(post("/admin/survey/options/" + id)
                        .param("label", "매우 건조")
                        .param("value", "0")
                        .param("sortOrder", "1")
                        .param("capsFeatureAt", "25")
                        .param("referDermatologist", "true"))
                .andExpect(status().is3xxRedirection());

        Integer cap = jdbcTemplate.queryForObject(
                "select caps_feature_at from survey_option where id = ?", Integer.class, id);
        Boolean refer = jdbcTemplate.queryForObject(
                "select refer_dermatologist from survey_option where id = ?", Boolean.class, id);
        org.assertj.core.api.Assertions.assertThat(cap).isEqualTo(25);
        org.assertj.core.api.Assertions.assertThat(refer).isTrue();
    }
}
