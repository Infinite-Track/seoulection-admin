package com.seoulection.admin.survey.application.dto;

/**
 * 문항을 묶을 수 있는 피부 지표 하나 — {@code skin_score_policy}의 행. 화면의 드롭다운이 이걸로 그려진다.
 *
 * @param scoreWeightSum    이 지표에 묶인 <b>활성 SCORE</b> 문항의 비중 합. 100이 아니어도 api-server가
 *                          "답한 문항의 비중 합"으로 나누므로 계산은 되지만, 관리자가 의도를 확인할 수 있게 보여준다.
 * @param minAnsweredWeight 답한 비중이 이 비율 미만이면 그 지표를 계산하지 않는다(정책 화면에서 편집).
 */
public record SurveyFeatureResult(String featureKey, String label, int scoreWeightSum, double minAnsweredWeight) {
}
