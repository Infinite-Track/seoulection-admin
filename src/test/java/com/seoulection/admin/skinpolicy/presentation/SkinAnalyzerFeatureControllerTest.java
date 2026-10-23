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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 분석기별 사진 신뢰 지표 화면 — 실제 Postgres 로 왕복. 스키마는 어드민 매핑으로 만든 테스트용. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SkinAnalyzerFeatureControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from skin_analyzer_feature");
        jdbcTemplate.update("delete from skin_score_policy");
        jdbcTemplate.update("""
                insert into skin_score_policy (feature_key, label, survey_reliability, photo_reliability,
                    inconsistent_survey_factor, bad_boundary, good_boundary, policy_version, min_answered_weight)
                values ('pores_score', 'Pores', 0, 1, 0.7, 0.74, 0.89, 'v1', 0.5),
                       ('wrinkle_score', 'Wrinkles', 0, 1, 0.7, 0.67, 0.84, 'v1', 0.5),
                       ('water_score', 'Hydration', 1, 0, 0.7, 0.25, 0.75, 'v1', 0.5)""");
        jdbcTemplate.update("insert into skin_analyzer_feature (analyzer, feature_key) values ('ailabtools', 'pores_score'), ('ailabtools', 'wrinkle_score')");
    }

    @Test
    @DisplayName("화면은 데이터에 있는 분석기와 코드에 있는 분석기(행 없음)를 모두 그리고, 체크 상태를 보여준다")
    void page_rendersMatrix() throws Exception {
        mockMvc.perform(get("/admin/skin-analyzer-features"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("ailabtools")))
                .andExpect(content().string(containsString("mock")))
                .andExpect(content().string(containsString("행 없음 — 코드 고정 목록 사용")))
                .andExpect(content().string(containsString("pores_score")));
    }

    @Test
    @DisplayName("저장은 집합을 통째로 바꾼다 — 체크 해제한 지표는 사라지고 새로 체크한 지표가 들어간다")
    void replace_setsExactly() throws Exception {
        mockMvc.perform(post("/admin/skin-analyzer-features/ailabtools")
                        .param("featureKeys", "wrinkle_score")
                        .param("featureKeys", "water_score"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("successMessage"));

        List<String> keys = jdbcTemplate.queryForList(
                "select feature_key from skin_analyzer_feature where analyzer = 'ailabtools' order by 1", String.class);
        assertThat(keys).containsExactly("water_score", "wrinkle_score");
    }

    @Test
    @DisplayName("새 분석기 추가 — 이름은 소문자 규칙, 없는 지표는 거부")
    void add_validates() throws Exception {
        mockMvc.perform(post("/admin/skin-analyzer-features")
                        .param("analyzer", "model")
                        .param("featureKeys", "pores_score"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("successMessage"));
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from skin_analyzer_feature where analyzer = 'model'", Long.class)).isEqualTo(1L);

        mockMvc.perform(post("/admin/skin-analyzer-features")
                        .param("analyzer", "Bad Name")
                        .param("featureKeys", "pores_score"))
                .andExpect(flash().attributeExists("errorMessage"));
        mockMvc.perform(post("/admin/skin-analyzer-features/mock")
                        .param("featureKeys", "nope_score"))
                .andExpect(flash().attributeExists("errorMessage"));
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from skin_analyzer_feature where analyzer = 'mock'", Long.class)).isZero();
    }

    @Test
    @DisplayName("전부 해제하면 행이 없어진다(서버는 코드 고정 목록으로 돌아간다)")
    void replace_empty_removesRows() throws Exception {
        mockMvc.perform(post("/admin/skin-analyzer-features/ailabtools"))
                .andExpect(status().is3xxRedirection());
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from skin_analyzer_feature where analyzer = 'ailabtools'", Long.class)).isZero();
    }
}
