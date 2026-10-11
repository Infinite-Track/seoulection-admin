package com.seoulection.admin.survey.domain.entity;

/**
 * 문항의 답이 추천 계산에서 <b>어떻게 쓰이는가</b>. api-server의 같은 이름 enum과 값이 같아야 한다
 * (DB CHECK 제약 {@code role in ('SCORE','MODIFIER','FLAG')}).
 *
 * <p>노출 시점을 뜻하는 {@code tier}(CORE / PHOTO_SUBSTITUTE)와는 다른 축이라 이름을 SCORE로 둔다.
 */
public enum SurveyQuestionRole {
    /** 지표 점수 산식에 들어간다. 같은 지표의 SCORE 문항끼리 weight로 가중 평균한다. */
    SCORE("점수 산식"),
    /** 산식에는 넣지 않고 보조값으로만 저장한다(선택 문항). */
    MODIFIER("보조값"),
    /** 점수가 없다. 회피 성분·피부타입 확인·진료 안내 같은 표시·규칙용. */
    FLAG("표시·규칙");

    private final String label;

    SurveyQuestionRole(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static SurveyQuestionRole fromNullable(String value) {
        if (value == null || value.isBlank()) {
            return SCORE;
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("역할은 SCORE / MODIFIER / FLAG 중 하나여야 합니다.");
        }
    }
}
