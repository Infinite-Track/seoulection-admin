package com.seoulection.admin.survey.application.dto;

import com.seoulection.admin.survey.domain.entity.SurveyQuestion;
import com.seoulection.admin.survey.domain.entity.SurveyQuestionRole;

import java.util.List;

/**
 * 문항 하나와 그 선택지 전부(비활성 포함 — 관리자는 숨긴 것도 보고 되살릴 수 있어야 한다).
 *
 * @param featureKey   묶인 지표. 없으면 null(점수에 안 쓰임).
 * @param featureLabel 지표의 표시 이름({@code skin_score_policy.label}). 지표가 없거나 정책 행이 없으면 null.
 * @param weight       같은 지표 안에서의 비중(0~100).
 * @param role         SCORE / MODIFIER / FLAG.
 */
public record SurveyQuestionResult(
        String key,
        String title,
        boolean active,
        String featureKey,
        String featureLabel,
        int weight,
        SurveyQuestionRole role,
        List<SurveyOptionResult> options
) {

    public static SurveyQuestionResult of(SurveyQuestion question, String featureLabel,
                                          List<SurveyOptionResult> options) {
        return new SurveyQuestionResult(question.getQuestionKey(), question.getTitle(), question.isActive(),
                question.getFeatureKey(), featureLabel, question.getWeight(), question.getRole(), options);
    }

    /** 점수 산식에 들어가는 문항인가 — 화면에서 배지 색을 가른다. */
    public boolean scoresFeature() {
        return featureKey != null && role == SurveyQuestionRole.SCORE;
    }
}
