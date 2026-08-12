package com.seoulection.admin.skinpolicy.domain;

public record SkinScorePolicy(String featureKey, String label, double surveyReliability,
                              double photoReliability, double inconsistentSurveyFactor,
                              String policyVersion) {
    public SkinScorePolicy {
        validate(surveyReliability, "설문 신뢰도");
        validate(photoReliability, "사진 신뢰도");
        validate(inconsistentSurveyFactor, "불일치 계수");
        if (surveyReliability + photoReliability <= 0) {
            throw new IllegalArgumentException("설문·사진 신뢰도가 모두 0일 수 없습니다.");
        }
        if (policyVersion == null || policyVersion.isBlank()) {
            throw new IllegalArgumentException("정책 버전은 비워둘 수 없습니다.");
        }
    }

    private static void validate(double value, String name) {
        if (value < 0 || value > 1) throw new IllegalArgumentException(name + "는 0~1이어야 합니다.");
    }
}
