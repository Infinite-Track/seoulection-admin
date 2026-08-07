package com.seoulection.admin.survey.application.dto;

import com.seoulection.admin.survey.domain.entity.SurveyQuestion;

import java.util.List;

/** 문항 하나와 그 선택지 전부(비활성 포함 — 관리자는 숨긴 것도 보고 되살릴 수 있어야 한다). */
public record SurveyQuestionResult(
        String key,
        String title,
        List<SurveyOptionResult> options
) {

    public static SurveyQuestionResult of(SurveyQuestion question, List<SurveyOptionResult> options) {
        return new SurveyQuestionResult(question.getQuestionKey(), question.getTitle(), options);
    }
}
