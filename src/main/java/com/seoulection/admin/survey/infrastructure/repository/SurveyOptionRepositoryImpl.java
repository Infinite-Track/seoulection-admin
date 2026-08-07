package com.seoulection.admin.survey.infrastructure.repository;

import com.seoulection.admin.survey.domain.entity.SurveyOption;
import com.seoulection.admin.survey.domain.repository.SurveyOptionRepository;
import com.seoulection.admin.survey.infrastructure.entity.SurveyOptionJpaEntity;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** 선택지 저장소 포트의 JPA 어댑터. 경계에서 도메인↔영속 엔티티를 변환한다. */
@Repository
public class SurveyOptionRepositoryImpl implements SurveyOptionRepository {

    private final SurveyOptionJpaRepository jpaRepository;

    public SurveyOptionRepositoryImpl(SurveyOptionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<SurveyOption> findByQuestionKey(String questionKey) {
        // 정렬 기준(sortOrder → code)은 api-server의 SurveyOptionCatalog와 같아야 한다.
        // 관리 화면에서 보이는 순서가 곧 사용자 화면 순서여야 관리자가 결과를 예측할 수 있다.
        return jpaRepository.findByQuestionKeyOrderBySortOrderAscCodeAsc(questionKey).stream()
                .map(SurveyOptionJpaEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<SurveyOption> findById(Long id) {
        return jpaRepository.findById(id).map(SurveyOptionJpaEntity::toDomain);
    }

    @Override
    public boolean existsByQuestionKeyAndCode(String questionKey, String code) {
        return jpaRepository.existsByQuestionKeyAndCode(questionKey, code);
    }

    @Override
    public SurveyOption save(SurveyOption option) {
        return jpaRepository.save(SurveyOptionJpaEntity.fromDomain(option)).toDomain();
    }
}
