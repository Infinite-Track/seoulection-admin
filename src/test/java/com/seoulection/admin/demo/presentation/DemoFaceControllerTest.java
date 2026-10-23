package com.seoulection.admin.demo.presentation;

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

/** 데모 얼굴 관리 화면 — 실제 Postgres 로 왕복. 스키마는 어드민 매핑으로 만든 테스트용. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class DemoFaceControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from demo_face_profile_score");
        jdbcTemplate.update("delete from demo_face_profile");
        jdbcTemplate.update("delete from skin_score_policy");
        jdbcTemplate.update("""
                insert into skin_score_policy (feature_key, label, survey_reliability, photo_reliability,
                    inconsistent_survey_factor, bad_boundary, good_boundary, policy_version, min_answered_weight)
                values ('wrinkle_score', 'Wrinkles', 0, 1, 0.7, 0.67, 0.84, 'v1', 0.5),
                       ('water_score', 'Hydration', 1, 0, 0.7, 0.25, 0.75, 'v1', 0.5)""");
        jdbcTemplate.update("insert into demo_face_profile (code, label, image_url, sort_order, active) values ('DEMO_WRINKLES', 'Wrinkles', '/demo-faces/wrinkles.jpg', 1, true)");
        jdbcTemplate.update("insert into demo_face_profile_score (code, feature_key, score) values ('DEMO_WRINKLES', 'wrinkle_score', 0.2), ('DEMO_WRINKLES', 'water_score', 0.75)");
    }

    @Test
    @DisplayName("화면은 얼굴 카드와 지표별 점수 칸을 그린다")
    void page_renders() throws Exception {
        mockMvc.perform(get("/admin/demo-faces"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("DEMO_WRINKLES")))
                .andExpect(content().string(containsString("name=\"score[wrinkle_score]\"")))
                .andExpect(content().string(containsString("value=\"0.20\"")));
    }

    @Test
    @DisplayName("저장은 이름·이미지·노출과 점수를 통째로 고친다")
    void update_savesScores() throws Exception {
        mockMvc.perform(post("/admin/demo-faces/DEMO_WRINKLES")
                        .param("label", "Fine lines")
                        .param("imageUrl", "/demo-faces/wrinkles.jpg")
                        .param("sortOrder", "3")
                        .param("active", "true")
                        .param("score[wrinkle_score]", "0.1")
                        .param("score[water_score]", "0.5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("successMessage"));

        Double wrinkle = jdbcTemplate.queryForObject(
                "select score from demo_face_profile_score where code = 'DEMO_WRINKLES' and feature_key = 'wrinkle_score'", Double.class);
        String label = jdbcTemplate.queryForObject("select label from demo_face_profile where code = 'DEMO_WRINKLES'", String.class);
        assertThat(wrinkle).isEqualTo(0.1);
        assertThat(label).isEqualTo("Fine lines");
    }

    @Test
    @DisplayName("새 얼굴은 점수가 전부 0.75 로 시작하고, 중복 코드·없는 지표·범위 밖 점수는 거부된다")
    void create_and_validation() throws Exception {
        mockMvc.perform(post("/admin/demo-faces")
                        .param("code", "demo_redness").param("label", "Redness").param("imageUrl", "/demo-faces/r.jpg"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("successMessage"));
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from demo_face_profile_score where code = 'DEMO_REDNESS' and score = 0.75", Long.class)).isEqualTo(2L);

        mockMvc.perform(post("/admin/demo-faces")
                        .param("code", "DEMO_WRINKLES").param("label", "x").param("imageUrl", "/x.jpg"))
                .andExpect(flash().attributeExists("errorMessage"));
        mockMvc.perform(post("/admin/demo-faces/DEMO_WRINKLES")
                        .param("label", "Wrinkles").param("imageUrl", "/demo-faces/wrinkles.jpg")
                        .param("score[wrinkle_score]", "1.5"))
                .andExpect(flash().attributeExists("errorMessage"));
    }
}
