package com.seoulection.admin.survey.infrastructure.repository;

import com.seoulection.admin.survey.domain.enums.SurveyQuestionKey;
import com.seoulection.admin.survey.infrastructure.entity.SurveyOptionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Spring Data JPA 리포지토리(영속성 엔티티 전용, 내부용). */
interface SurveyOptionJpaRepository extends JpaRepository<SurveyOptionJpaEntity, Long> {

    List<SurveyOptionJpaEntity> findByQuestionKeyOrderBySortOrderAscCodeAsc(SurveyQuestionKey questionKey);

    boolean existsByQuestionKeyAndCode(SurveyQuestionKey questionKey, String code);
}
