package com.seoulection.admin.skinpolicy.infrastructure;

import com.seoulection.admin.skinpolicy.domain.SkinScorePolicy;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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
    }

    public static SkinScorePolicyJpaEntity from(SkinScorePolicy policy) {
        return new SkinScorePolicyJpaEntity(policy);
    }

    public SkinScorePolicy toDomain() {
        return new SkinScorePolicy(featureKey, label, surveyReliability, photoReliability,
                inconsistentSurveyFactor, badBoundary, goodBoundary, policyVersion);
    }
}
