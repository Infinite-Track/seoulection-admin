package com.seoulection.admin.survey.domain.entity;

import java.util.regex.Pattern;

/**
 * 설문 문항 — 관리자가 CRUD하는 대상(추가·문구 수정·숨김·지표 매핑).
 *
 * <p>{@code questionKey}는 문자열 자연 PK다(api-server와 동일). 예전엔 {@code SurveyQuestionKey} enum이었지만,
 * api-server의 제출 요청이 문항마다 고정 필드를 갖는 구조를 벗어나 문항 목록 형태로 바뀌면서 문항 집합을
 * 코드에 고정할 이유가 사라졌다 — 지금은 {@link SurveyOption}과 마찬가지로 행의 추가·숨김이 데이터만으로 가능하다.
 *
 * <p><b>지표 매핑</b>({@code featureKey} / {@code weight} / {@code role}, api-server V30): 이 문항의 답이 어느 피부
 * 지표에 몇 %로 들어가는지. 전에는 api-server 코드에 고정돼 있어 어드민에서 만든 문항이 추천 계산에 조용히
 * 빠졌다. {@code featureKey}는 {@code skin_score_policy.feature_key}를 가리키는 FK라 자유 입력이 아니다 —
 * 비워 두면(null) 점수에 쓰이지 않는다.
 *
 * <p>삭제는 {@link SurveyOption}과 같은 이유로 {@link #withActive}(soft delete)만 제공한다 — 이미 이 문항으로
 * 답한 {@code survey_answer} 행이 있어, 하드 삭제하면 그 응답이 가리키는 문항 문구를 되찾을 수 없다.
 * 숨긴 문항의 답도 api-server가 계속 쓴다(새 문항에 답하기 전까지).
 */
public class SurveyQuestion {

    /** 대문자 스네이크. {@link SurveyOption#getCode()}와 같은 규칙 — api-server가 안정 키로 다룬다. */
    private static final Pattern QUESTION_KEY_FORMAT = Pattern.compile("^[A-Z][A-Z0-9_]*$");

    public static final int DEFAULT_WEIGHT = 100;

    private final String questionKey;
    private final String title;
    private final int sortOrder;
    private final boolean active;
    private final String featureKey;
    private final int weight;
    private final SurveyQuestionRole role;

    private SurveyQuestion(String questionKey, String title, int sortOrder, boolean active,
                           String featureKey, int weight, SurveyQuestionRole role) {
        this.questionKey = requireQuestionKey(questionKey);
        this.title = requireText(title);
        this.sortOrder = sortOrder;
        this.active = active;
        this.featureKey = normalizeFeatureKey(featureKey);
        this.weight = requireWeight(weight);
        this.role = role == null ? SurveyQuestionRole.SCORE : role;
    }

    /** 새 문항. 활성 상태로 시작한다. */
    public static SurveyQuestion create(String questionKey, String title, int sortOrder,
                                        String featureKey, int weight, SurveyQuestionRole role) {
        return new SurveyQuestion(questionKey, title, sortOrder, true, featureKey, weight, role);
    }

    /** 지표에 묶지 않는 문항(표시용). 옛 호출부 호환. */
    public static SurveyQuestion create(String questionKey, String title, int sortOrder) {
        return create(questionKey, title, sortOrder, null, DEFAULT_WEIGHT, SurveyQuestionRole.FLAG);
    }

    /** 저장소가 읽어온 기존 행을 복원할 때 쓴다. */
    public static SurveyQuestion of(String questionKey, String title, int sortOrder, boolean active,
                                    String featureKey, int weight, SurveyQuestionRole role) {
        return new SurveyQuestion(questionKey, title, sortOrder, active, featureKey, weight, role);
    }

    public SurveyQuestion withTitle(String newTitle) {
        return new SurveyQuestion(questionKey, newTitle, sortOrder, active, featureKey, weight, role);
    }

    /** 지표·비중·역할만 바꾼다. 문구·순서·노출은 그대로. */
    public SurveyQuestion withMapping(String newFeatureKey, int newWeight, SurveyQuestionRole newRole) {
        return new SurveyQuestion(questionKey, title, sortOrder, active, newFeatureKey, newWeight, newRole);
    }

    /** 노출/숨김. {@code false}가 곧 삭제다(행은 남는다). */
    public SurveyQuestion withActive(boolean newActive) {
        return new SurveyQuestion(questionKey, title, sortOrder, newActive, featureKey, weight, role);
    }

    /** 점수 산식에 들어가는 문항인가(지표가 있고 역할이 SCORE). */
    public boolean scoresFeature() {
        return featureKey != null && role == SurveyQuestionRole.SCORE;
    }

    private static String requireQuestionKey(String questionKey) {
        if (questionKey == null || questionKey.isBlank()) {
            throw new IllegalArgumentException("문항 키를 입력해 주세요.");
        }
        String trimmed = questionKey.trim();
        if (!QUESTION_KEY_FORMAT.matcher(trimmed).matches()) {
            throw new IllegalArgumentException(
                    "문항 키는 영문 대문자·숫자·밑줄만 쓸 수 있습니다(예: SKIN_TYPE_CHECK).");
        }
        return trimmed;
    }

    private static String requireText(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("질문 문구를 입력해 주세요.");
        }
        return title.trim();
    }

    /** 빈 문자열(폼의 "선택 안 함")은 null로 — "지표 없음"이 한 가지 표현만 갖게. */
    private static String normalizeFeatureKey(String featureKey) {
        if (featureKey == null || featureKey.isBlank()) {
            return null;
        }
        return featureKey.trim();
    }

    private static int requireWeight(int weight) {
        if (weight < 0 || weight > 100) {
            throw new IllegalArgumentException("비중은 0에서 100 사이여야 합니다.");
        }
        return weight;
    }

    public String getQuestionKey() {
        return questionKey;
    }

    public String getTitle() {
        return title;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public boolean isActive() {
        return active;
    }

    public String getFeatureKey() {
        return featureKey;
    }

    public int getWeight() {
        return weight;
    }

    public SurveyQuestionRole getRole() {
        return role;
    }
}
