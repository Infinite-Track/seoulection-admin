package com.seoulection.admin.skinpolicy.infrastructure;

import com.seoulection.admin.skinpolicy.domain.SkinScorePolicy;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** {@code skin_score_policy} — 지표 10개의 마스터. 스키마 주인은 api-server(여기는 {@code ddl-auto=none}). */
@Entity
@Table(name = "skin_score_policy")
public class SkinScorePolicyJpaEntity {
    @Id @Column(name = "feature_key", length = 64)
    private String featureKey;
    @Column(nullable = false, length = 64)
    private String label;
    @Column(name = "survey_reliability", nullable = false)
    private double surveyReliability;
    @Column(name = "photo_reliability", nullable = false)
    private double photoReliability;
    @Column(name = "inconsistent_survey_factor", nullable = false)
    private double inconsistentSurveyFactor;
    @Column(name = "bad_boundary", nullable = false)
    private double badBoundary;
    @Column(name = "good_boundary", nullable = false)
    private double goodBoundary;
    @Column(name = "policy_version", nullable = false, length = 32)
    private String policyVersion;
    /** api-server V30. 기본값은 테스트 스키마(이 매핑으로 생성)와 운영이 같게. */
    @Column(name = "min_answered_weight", nullable = false, columnDefinition = "float(53) default 0.5")
    private double minAnsweredWeight;
    /** 설문 점수용 경계(api-server V36). null 이면 사진 경계를 쓴다. */
    @Column(name = "survey_bad_boundary")
    private Double surveyBadBoundary;
    @Column(name = "survey_good_boundary")
    private Double surveyGoodBoundary;

    protected SkinScorePolicyJpaEntity() {}

    private SkinScorePolicyJpaEntity(SkinScorePolicy policy) {
        this.featureKey = policy.featureKey();
        this.label = policy.label();
        this.surveyReliability = policy.surveyReliability();
        this.photoReliability = policy.photoReliability();
        this.inconsistentSurveyFactor = policy.inconsistentSurveyFactor();
        this.badBoundary = policy.badBoundary();
        this.goodBoundary = policy.goodBoundary();
        this.policyVersion = policy.policyVersion();
        this.minAnsweredWeight = policy.minAnsweredWeight();
        this.surveyBadBoundary = policy.surveyBadBoundary();
        this.surveyGoodBoundary = policy.surveyGoodBoundary();
    }

    public static SkinScorePolicyJpaEntity from(SkinScorePolicy policy) {
        return new SkinScorePolicyJpaEntity(policy);
    }

    public SkinScorePolicy toDomain() {
        return new SkinScorePolicy(featureKey, label, surveyReliability, photoReliability,
                inconsistentSurveyFactor, badBoundary, goodBoundary, policyVersion, minAnsweredWeight,
                surveyBadBoundary, surveyGoodBoundary);
    }
}
