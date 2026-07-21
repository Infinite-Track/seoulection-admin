package com.seoulection.admin.survey.domain.enums;

/**
 * 설문 문항 식별자. <b>api-server의 동명 enum과 상수명이 일치해야 한다</b> —
 * {@code survey_question.question_key} / {@code survey_option.question_key} 컬럼에 문자열로 저장되기 때문이다.
 *
 * <p>여기에 상수를 추가해도 api-server가 모르면 아무 일도 일어나지 않는다. 문항 집합은 양쪽 코드에
 * 고정돼 있고, 관리자가 바꾸는 건 <b>문구와 선택지</b>다.
 */
public enum SurveyQuestionKey {

    /** Q1 — 회피 성분/상태. 제출 요청의 {@code avoidances} 필드. */
    AVOIDANCE,

    /** Q2 — 피부 고민. 제출 요청의 {@code concerns} 필드. */
    CONCERN
}
