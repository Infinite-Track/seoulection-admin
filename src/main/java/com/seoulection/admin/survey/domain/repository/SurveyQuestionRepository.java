package com.seoulection.admin.survey.domain.repository;

import com.seoulection.admin.survey.domain.entity.SurveyQuestion;

import java.util.List;
import java.util.Optional;

/** 문항 마스터 저장소 포트. 구현은 {@code infrastructure.repository.SurveyQuestionRepositoryImpl}. */
public interface SurveyQuestionRepository {

    List<SurveyQuestion> findAllOrdered();

    Optional<SurveyQuestion> findByKey(String questionKey);

    SurveyQuestion save(SurveyQuestion question);
}
