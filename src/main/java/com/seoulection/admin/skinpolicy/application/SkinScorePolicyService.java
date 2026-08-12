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

    @Transactional
    public void update(String featureKey, double surveyReliability, double photoReliability,
                       double inconsistentSurveyFactor, String policyVersion) {
        SkinScorePolicy current = repository.findById(featureKey)
                .orElseThrow(() -> new IllegalArgumentException("없는 피부 항목입니다: " + featureKey))
                .toDomain();
        repository.save(SkinScorePolicyJpaEntity.from(new SkinScorePolicy(featureKey, current.label(),
                surveyReliability, photoReliability, inconsistentSurveyFactor, policyVersion.trim())));
    }
}
