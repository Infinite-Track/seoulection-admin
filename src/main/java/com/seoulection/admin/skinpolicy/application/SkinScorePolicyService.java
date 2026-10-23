package com.seoulection.admin.skinpolicy.application;

import com.seoulection.admin.skinpolicy.domain.SkinScorePolicy;
import com.seoulection.admin.skinpolicy.infrastructure.SkinScorePolicyJpaEntity;
import com.seoulection.admin.skinpolicy.infrastructure.SkinScorePolicyJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SkinScorePolicyService {
    private final SkinScorePolicyJpaRepository repository;

    public SkinScorePolicyService(SkinScorePolicyJpaRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<SkinScorePolicy> findAll() {
        return repository.findAll().stream().map(SkinScorePolicyJpaEntity::toDomain)
                .sorted(java.util.Comparator.comparing(SkinScorePolicy::featureKey)).toList();
    }

    /**
     * 경계·버전·최소 답변 비중만 바꾼다. 신뢰도(점수 원천)는 그대로 — 그건 코드가 결정하는 값이다.
     *
     * @param minAnsweredWeight 설문 리졸버가 이 지표를 계산할 최소 답변 비중(0~1). 문항을 늘린 지표
     *                          (예: 민감 5문항)에서 "4문항 이상 답해야"를 0.8로 표현한다.
     */
    @Transactional
    public void update(String featureKey, double badBoundary, double goodBoundary, String policyVersion,
                       double minAnsweredWeight) {
        SkinScorePolicy current = repository.findById(featureKey)
                .orElseThrow(() -> new IllegalArgumentException("없는 피부 항목입니다: " + featureKey))
                .toDomain();
        update(featureKey, badBoundary, goodBoundary, policyVersion, minAnsweredWeight,
                current.surveyBadBoundary(), current.surveyGoodBoundary());
    }

    /**
     * 경계는 원천별로 둘이다(api-server V36): bad/good 은 사진 점수용(분포의 P30/P70), survey* 는 설문 점수용(25/75).
     * 설문 경계를 비우면(null) 설문 점수도 사진 경계로 판정된다.
     */
    @Transactional
    public void update(String featureKey, double badBoundary, double goodBoundary, String policyVersion,
                       double minAnsweredWeight, Double surveyBadBoundary, Double surveyGoodBoundary) {
        SkinScorePolicy current = repository.findById(featureKey)
                .orElseThrow(() -> new IllegalArgumentException("없는 피부 항목입니다: " + featureKey))
                .toDomain();
        repository.save(SkinScorePolicyJpaEntity.from(new SkinScorePolicy(featureKey, current.label(),
                current.surveyReliability(), current.photoReliability(), current.inconsistentSurveyFactor(),
                badBoundary, goodBoundary, policyVersion.trim(), minAnsweredWeight,
                surveyBadBoundary, surveyGoodBoundary)));
    }

    /** 최소 답변 비중은 그대로 두는 수정. 옛 호출부 호환. */
    @Transactional
    public void update(String featureKey, double badBoundary, double goodBoundary, String policyVersion) {
        SkinScorePolicy current = repository.findById(featureKey)
                .orElseThrow(() -> new IllegalArgumentException("없는 피부 항목입니다: " + featureKey))
                .toDomain();
        update(featureKey, badBoundary, goodBoundary, policyVersion, current.minAnsweredWeight());
    }
}
