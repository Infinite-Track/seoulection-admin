package com.seoulection.admin.skinpolicy.domain;

/**
 * @param minAnsweredWeight 설문 리졸버가 이 지표를 계산하려면 답한 SCORE 문항 비중이 전체의 몇 이상이어야 하나(0~1).
 *                          예: 민감 5문항(각 20%)에서 0.8 → 4문항 이상 답해야 점수가 나온다. api-server V30.
 */
public record SkinScorePolicy(String featureKey, String label, double surveyReliability,
                              double photoReliability, double inconsistentSurveyFactor,
                              double badBoundary, double goodBoundary,
                              String policyVersion, double minAnsweredWeight) {

    public static final double DEFAULT_MIN_ANSWERED_WEIGHT = 0.5;

    public SkinScorePolicy {
        validate(surveyReliability, "설문 신뢰도");
        validate(photoReliability, "사진 신뢰도");
        validate(inconsistentSurveyFactor, "불일치 계수");
        validate(badBoundary, "Bad 경계");
        validate(goodBoundary, "Good 경계");
        validate(minAnsweredWeight, "최소 답변 비중");
        if (badBoundary > goodBoundary) {
            throw new IllegalArgumentException("Bad 경계는 Good 경계보다 클 수 없습니다.");
        }
        if (surveyReliability + photoReliability <= 0) {
            throw new IllegalArgumentException("설문·사진 신뢰도가 모두 0일 수 없습니다.");
        }
        if (policyVersion == null || policyVersion.isBlank()) {
            throw new IllegalArgumentException("정책 버전은 비워둘 수 없습니다.");
        }
    }

    /** 옛 호출부 호환 — 최소 답변 비중 기본값 0.5. */
    public SkinScorePolicy(String featureKey, String label, double surveyReliability,
                           double photoReliability, double inconsistentSurveyFactor,
                           double badBoundary, double goodBoundary, String policyVersion) {
        this(featureKey, label, surveyReliability, photoReliability, inconsistentSurveyFactor,
                badBoundary, goodBoundary, policyVersion, DEFAULT_MIN_ANSWERED_WEIGHT);
    }

    private static void validate(double value, String name) {
        if (value < 0 || value > 1) throw new IllegalArgumentException(name + "는 0~1이어야 합니다.");
    }
}
