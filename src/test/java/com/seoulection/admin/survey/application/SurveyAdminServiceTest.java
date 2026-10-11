package com.seoulection.admin.survey.application;

import com.seoulection.admin.TestcontainersConfiguration;
import com.seoulection.admin.survey.application.dto.SurveyOptionResult;
import com.seoulection.admin.survey.application.dto.SurveyQuestionResult;
import com.seoulection.admin.survey.application.dto.SurveyFeatureResult;
import com.seoulection.admin.survey.application.service.SurveyAdminService;
import com.seoulection.admin.survey.domain.entity.SurveyQuestionRole;
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
        jdbcTemplate.update("delete from skin_score_policy");
        // weight·role 은 넣지 않는다 — 운영(V30)과 같은 DB 기본값(100, SCORE)이 들어가는지도 함께 확인된다.
        jdbcTemplate.update("""
                insert into survey_question (question_key, title, sort_order, active)
                values ('AVOIDANCE', '회피 항목', 1, true), ('CONCERN', '피부 고민', 2, true)""");
        // 지표 마스터. 문항의 feature_key 는 이 중 하나여야 한다.
        jdbcTemplate.update("""
                insert into skin_score_policy (feature_key, label, survey_reliability, photo_reliability,
                    inconsistent_survey_factor, bad_boundary, good_boundary, policy_version, min_answered_weight)
                values ('water_score', 'Hydration', 1, 0, 0.7, 0.33, 1, 'v1', 0.5),
                       ('acne_score', 'Acne', 0, 1, 0.7, 0.3, 0.7, 'v1', 0.5)""");
    }

    private List<SurveyOptionResult> avoidanceOptions() {
        return service.getQuestions().stream()
                .filter(q -> q.key().equals("AVOIDANCE"))
                .findFirst().orElseThrow()
                .options();
    }

    @Test
    @DisplayName("문항을 추가하면 목록에 새 문항이 나타난다 — 문항 집합은 코드에 고정돼 있지 않다")
    void createQuestion_addsNewQuestion() {
        service.createQuestion("water_direct", "피부 수분 상태는 어떤가요?", 3);

        List<SurveyQuestionResult> questions = service.getQuestions();
        assertThat(questions).hasSize(3);
        assertThat(questions.stream().map(SurveyQuestionResult::key)).contains("WATER_DIRECT");
    }

    @Test
    @DisplayName("이미 있는 문항 키로 또 추가하면 거부된다 — 자연 PK라 그냥 저장하면 upsert로 덮어써 버린다")
    void createQuestion_duplicateKey_rejected() {
        assertThatThrownBy(() -> service.createQuestion("AVOIDANCE", "다른 문구", 9))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 있는 문항");
    }

    @Test
    @DisplayName("지표 없이 만든 문항은 FLAG · 비중 100 — 기존 호출부와 DB 기본값이 같다")
    void createQuestion_withoutFeature_isFlag() {
        service.createQuestion("NEW_FLAG", "표시용 문항", 3);

        SurveyQuestionResult saved = questionByKey("NEW_FLAG");
        assertThat(saved.featureKey()).isNull();
        assertThat(saved.weight()).isEqualTo(100);
        assertThat(saved.role()).isEqualTo(SurveyQuestionRole.FLAG);
        assertThat(saved.scoresFeature()).isFalse();
        // 시드로 넣은(컬럼 생략) 행은 DB 기본값 SCORE 로 읽힌다.
        assertThat(questionByKey("CONCERN").role()).isEqualTo(SurveyQuestionRole.SCORE);
    }

    @Test
    @DisplayName("★ 지표를 골라 만든 문항은 지표·비중·역할이 저장되고 지표 라벨이 함께 나온다 — 코드 수정 없이 추천 계산에 들어가는 길")
    void createQuestion_withFeature_savesMapping() {
        service.createQuestion("WATER_SITUATIONAL", "지난 7일 건조해 보인 날", 4, "water_score", 50,
                SurveyQuestionRole.SCORE);

        SurveyQuestionResult saved = questionByKey("WATER_SITUATIONAL");
        assertThat(saved.featureKey()).isEqualTo("water_score");
        assertThat(saved.featureLabel()).isEqualTo("Hydration");
        assertThat(saved.weight()).isEqualTo(50);
        assertThat(saved.role()).isEqualTo(SurveyQuestionRole.SCORE);
        assertThat(saved.scoresFeature()).isTrue();
    }

    @Test
    @DisplayName("정책에 없는 지표 키는 거부된다 — 드롭다운 밖의 값(오타·대소문자)이 들어오는 길을 막는다")
    void createQuestion_unknownFeature_rejected() {
        assertThatThrownBy(() -> service.createQuestion("WATER_X", "문구", 4, "Water", 50, SurveyQuestionRole.SCORE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("없는 피부 지표");
        assertThatThrownBy(() -> service.createQuestion("WATER_Y", "문구", 4, "water_score", 150, SurveyQuestionRole.SCORE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("비중");
    }

    @Test
    @DisplayName("매핑 수정은 지표·비중·역할만 바꾸고 문구·순서·노출은 그대로다")
    void updateQuestionMapping_changesOnlyMapping() {
        service.createQuestion("ACNE_NODULE", "결절이 있나요?", 5, "acne_score", 100, SurveyQuestionRole.SCORE);

        service.updateQuestionMapping("ACNE_NODULE", "acne_score", 0, SurveyQuestionRole.FLAG);

        SurveyQuestionResult updated = questionByKey("ACNE_NODULE");
        assertThat(updated.title()).isEqualTo("결절이 있나요?");
        assertThat(updated.active()).isTrue();
        assertThat(updated.weight()).isZero();
        assertThat(updated.role()).isEqualTo(SurveyQuestionRole.FLAG);
        assertThat(updated.scoresFeature()).isFalse();

        // 빈 문자열(폼의 "지표 없음")은 null 로 저장된다.
        service.updateQuestionMapping("ACNE_NODULE", "", 100, SurveyQuestionRole.FLAG);
        assertThat(questionByKey("ACNE_NODULE").featureKey()).isNull();
    }

    @Test
    @DisplayName("★ 문구 수정·숨김은 매핑을 건드리지 않는다 — upsert 가 지표·비중을 기본값으로 되돌리면 추천 계산이 조용히 바뀐다")
    void updateQuestionTitle_keepsMapping() {
        service.createQuestion("WATER_DIRECT", "평소 건조한 정도", 3, "water_score", 50, SurveyQuestionRole.SCORE);

        service.updateQuestionTitle("WATER_DIRECT", "바뀐 문구");
        service.changeQuestionActive("WATER_DIRECT", false);

        SurveyQuestionResult after = questionByKey("WATER_DIRECT");
        assertThat(after.title()).isEqualTo("바뀐 문구");
        assertThat(after.active()).isFalse();
        assertThat(after.featureKey()).isEqualTo("water_score");
        assertThat(after.weight()).isEqualTo(50);
        assertThat(after.role()).isEqualTo(SurveyQuestionRole.SCORE);
    }

    @Test
    @DisplayName("지표별 비중 합은 활성 · SCORE 문항만 더한다 — 숨긴 문항과 MODIFIER 는 빠진다")
    void getFeatures_sumsActiveScoreWeights() {
        service.createQuestion("WATER_DIRECT", "a", 3, "water_score", 50, SurveyQuestionRole.SCORE);
        service.createQuestion("WATER_SITUATIONAL", "b", 4, "water_score", 50, SurveyQuestionRole.SCORE);
        service.createQuestion("WATER_OLD", "c", 5, "water_score", 100, SurveyQuestionRole.SCORE);
        service.changeQuestionActive("WATER_OLD", false);
        service.createQuestion("WATER_RETURN", "d", 6, "water_score", 100, SurveyQuestionRole.MODIFIER);

        List<SurveyFeatureResult> features = service.getFeatures();

        assertThat(features).extracting(SurveyFeatureResult::featureKey).containsExactly("acne_score", "water_score");
        SurveyFeatureResult water = features.get(1);
        assertThat(water.label()).isEqualTo("Hydration");
        assertThat(water.scoreWeightSum()).isEqualTo(100);
        assertThat(water.minAnsweredWeight()).isEqualTo(0.5);
        assertThat(features.get(0).scoreWeightSum()).isZero();
    }

    @Test
    @DisplayName("새 문항은 활성 상태로 생성된다")
    void createQuestion_startsActive() {
        service.createQuestion("WATER_DIRECT", "피부 수분 상태는 어떤가요?", 3);

        assertThat(questionByKey("WATER_DIRECT").active()).isTrue();
    }

    @Test
    @DisplayName("★ 문항 숨김은 행을 지우지 않는다(soft delete) — 되살릴 수 있고 목록·과거 응답은 그대로 남는다")
    void changeQuestionActive_isSoftDelete() {
        service.changeQuestionActive("AVOIDANCE", false);

        // 관리 화면은 숨긴 문항도 계속 보여줘야 되살릴 수 있다.
        List<SurveyQuestionResult> questions = service.getQuestions();
        assertThat(questions).hasSize(2);
        assertThat(questionByKey("AVOIDANCE").active()).isFalse();
        Long rows = jdbcTemplate.queryForObject(
                "select count(*) from survey_question where question_key = 'AVOIDANCE'", Long.class);
        assertThat(rows).isEqualTo(1L);

        service.changeQuestionActive("AVOIDANCE", true);
        assertThat(questionByKey("AVOIDANCE").active()).isTrue();
    }

    @Test
    @DisplayName("없는 문항을 숨기려 하면 거부된다")
    void changeQuestionActive_unknownKey_rejected() {
        assertThatThrownBy(() -> service.changeQuestionActive("NOT_A_QUESTION", false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("없는 문항입니다");
    }

    private SurveyQuestionResult questionByKey(String key) {
        return service.getQuestions().stream()
                .filter(q -> q.key().equals(key))
                .findFirst().orElseThrow();
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
    @DisplayName("선택지의 지표 상한·진료 안내가 저장·수정된다 (결절 '예' → 여드름 ≤ 25 + 진료 안내)")
    void createOption_withCapAndReferral_roundTrips() {
        service.createOption("CONCERN", "NODULE_YES", "있어요", null, 1, false, 25, true);

        SurveyOptionResult saved = service.getQuestions().stream()
                .filter(q -> q.key().equals("CONCERN")).findFirst().orElseThrow().options().get(0);
        assertThat(saved.value()).isNull();
        assertThat(saved.capsFeatureAt()).isEqualTo(25);
        assertThat(saved.referDermatologist()).isTrue();

        // 옛 시그니처 수정은 상한·진료 안내를 보존한다.
        service.updateOption(saved.id(), "있어요(수정)", null, 1, false);
        SurveyOptionResult kept = service.getQuestions().stream()
                .filter(q -> q.key().equals("CONCERN")).findFirst().orElseThrow().options().get(0);
        assertThat(kept.capsFeatureAt()).isEqualTo(25);
        assertThat(kept.referDermatologist()).isTrue();

        service.updateOption(saved.id(), "있어요", null, 1, false, null, false);
        SurveyOptionResult cleared = service.getQuestions().stream()
                .filter(q -> q.key().equals("CONCERN")).findFirst().orElseThrow().options().get(0);
        assertThat(cleared.capsFeatureAt()).isNull();
        assertThat(cleared.referDermatologist()).isFalse();

        assertThatThrownBy(() -> service.createOption("CONCERN", "NODULE_NO", "없어요", null, 2, false, 101, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("지표 상한");
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
