package com.seoulection.admin.survey.infrastructure.repository;

import com.seoulection.admin.survey.domain.entity.SurveyQuestion;
import com.seoulection.admin.survey.domain.enums.SurveyQuestionKey;
import com.seoulection.admin.survey.domain.repository.SurveyQuestionRepository;
import com.seoulection.admin.survey.infrastructure.entity.SurveyQuestionJpaEntity;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** 문항 저장소 포트의 JPA 어댑터. */
@Repository
public class SurveyQuestionRepositoryImpl implements SurveyQuestionRepository {

    private final SurveyQuestionJpaRepository jpaRepository;

    public SurveyQuestionRepositoryImpl(SurveyQuestionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<SurveyQuestion> findAllOrdered() {
        return jpaRepository.findAllByOrderBySortOrderAsc().stream()
                .map(SurveyQuestionJpaEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<SurveyQuestion> findByKey(SurveyQuestionKey questionKey) {
        return jpaRepository.findById(questionKey).map(SurveyQuestionJpaEntity::toDomain);
    }

    @Override
    public SurveyQuestion save(SurveyQuestion question) {
        return jpaRepository.save(SurveyQuestionJpaEntity.fromDomain(question)).toDomain();
    }
}
