package com.seoulection.admin.skinpolicy.application;

import com.seoulection.admin.skinpolicy.domain.SkinTypeRule;
import com.seoulection.admin.skinpolicy.infrastructure.SkinTypeRuleJpaEntity;
import com.seoulection.admin.skinpolicy.infrastructure.SkinTypeRuleJpaRepository;
import com.seoulection.admin.survey.domain.entity.SurveyQuestion;
import com.seoulection.admin.survey.domain.repository.SurveyQuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 설문 피부타입 계산 규칙 관리. 저장 즉시 다음 추천 계산부터 반영된다(api-server 리졸버가 호출마다 읽는다).
 */
@Service
public class SkinTypeRuleService {

    private static final String OIL_FEATURE_KEY = "oily_intensity_score";

    private final SkinTypeRuleJpaRepository repository;
    private final SurveyQuestionRepository questionRepository;

    public SkinTypeRuleService(SkinTypeRuleJpaRepository repository, SurveyQuestionRepository questionRepository) {
        this.repository = repository;
        this.questionRepository = questionRepository;
    }

    /** 행이 없으면(V34 전 DB) V34 초깃값을 보여준다 — 저장하면 그때 행이 생긴다. */
    @Transactional(readOnly = true)
    public SkinTypeRule get() {
        return repository.findById(SkinTypeRuleJpaEntity.SINGLETON_ID)
                .map(SkinTypeRuleJpaEntity::toDomain)
                .orElse(SkinTypeRule.DEFAULT);
    }

    /** T존·볼로 고를 수 있는 문항 — 유분 지표에 묶인 활성 문항. 다른 지표 문항을 T존으로 두면 뜻이 없다. */
    @Transactional(readOnly = true)
    public List<SurveyQuestion> oilQuestionCandidates() {
        return questionRepository.findAllOrdered().stream()
                .filter(SurveyQuestion::isActive)
                .filter(q -> OIL_FEATURE_KEY.equals(q.getFeatureKey()))
                .toList();
    }

    @Transactional
    public void update(String tZoneQuestionKey, String cheekQuestionKey, int oilyMax, int clearMin,
                       int dryWaterMax, boolean preferSelfDeclared) {
        SkinTypeRule rule = new SkinTypeRule(tZoneQuestionKey, cheekQuestionKey, oilyMax, clearMin, dryWaterMax,
                preferSelfDeclared);
        requireExisting(rule.tZoneQuestionKey());
        requireExisting(rule.cheekQuestionKey());
        repository.save(SkinTypeRuleJpaEntity.from(rule));
    }

    /** 운영 DB 는 FK 가 막지만 롤백 시점에야 터진다. 여기서 먼저 걸러 화면에 사유를 보여준다. */
    private void requireExisting(String questionKey) {
        if (!questionRepository.existsByKey(questionKey)) {
            throw new IllegalArgumentException("없는 문항입니다: " + questionKey);
        }
    }
}
