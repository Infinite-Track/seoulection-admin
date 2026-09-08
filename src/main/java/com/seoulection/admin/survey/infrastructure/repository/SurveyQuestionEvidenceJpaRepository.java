package com.seoulection.admin.survey.infrastructure.repository;

import com.seoulection.admin.survey.infrastructure.entity.SurveyQuestionEvidenceJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SurveyQuestionEvidenceJpaRepository extends JpaRepository<SurveyQuestionEvidenceJpaEntity, Long> {

    List<SurveyQuestionEvidenceJpaEntity> findByQuestionKeyOrderBySortOrderAscIdAsc(String questionKey);

    /** 문항별로 나눠 담기 위해 한 번에 읽는다 — 문항마다 쿼리를 쏘면 문항 수만큼 늘어난다. */
    List<SurveyQuestionEvidenceJpaEntity> findAllByOrderByQuestionKeyAscSortOrderAscIdAsc();
}
