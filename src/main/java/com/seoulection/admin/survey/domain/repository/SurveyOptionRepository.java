package com.seoulection.admin.survey.domain.repository;

import com.seoulection.admin.survey.domain.entity.SurveyOption;

import java.util.List;
import java.util.Optional;

/** 선택지 마스터 저장소 포트. 구현은 {@code infrastructure.repository.SurveyOptionRepositoryImpl}. */
public interface SurveyOptionRepository {

    /** 노출 순서로 정렬된 문항별 선택지(비활성 포함 — 관리 화면은 숨긴 것도 봐야 한다). */
    List<SurveyOption> findByQuestionKey(String questionKey);

    Optional<SurveyOption> findById(Long id);

    boolean existsByQuestionKeyAndCode(String questionKey, String code);

    SurveyOption save(SurveyOption option);
}
