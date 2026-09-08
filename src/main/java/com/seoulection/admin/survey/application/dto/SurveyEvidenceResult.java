package com.seoulection.admin.survey.application.dto;

import com.seoulection.admin.survey.infrastructure.entity.SurveyQuestionEvidenceJpaEntity;

/** 화면에 뿌릴 근거 한 줄. */
public record SurveyEvidenceResult(Long id, String questionKey, String title, String rationale,
                                   String url, String sourceType, Integer sortOrder) {

    public static SurveyEvidenceResult from(SurveyQuestionEvidenceJpaEntity entity) {
        return new SurveyEvidenceResult(entity.getId(), entity.getQuestionKey(), entity.getTitle(),
                entity.getRationale(), entity.getUrl(), entity.getSourceType(), entity.getSortOrder());
    }

    /** 링크 없는 내부 자료가 있다 — 화면이 빈 링크를 그리지 않게 판단을 여기서 한다. */
    public boolean hasUrl() {
        return url != null && !url.isBlank();
    }

    public String sourceLabel() {
        return switch (sourceType == null ? "" : sourceType) {
            case "PAPER" -> "논문";
            case "GUIDELINE" -> "가이드라인";
            case "CLINICAL" -> "임상";
            case "INTERNAL" -> "내부 자료";
            default -> "기사·기타";
        };
    }
}
