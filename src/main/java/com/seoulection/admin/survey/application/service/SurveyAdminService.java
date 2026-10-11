package com.seoulection.admin.survey.application.service;

import com.seoulection.admin.skinpolicy.domain.SkinScorePolicy;
import com.seoulection.admin.skinpolicy.infrastructure.SkinScorePolicyJpaEntity;
import com.seoulection.admin.skinpolicy.infrastructure.SkinScorePolicyJpaRepository;
import com.seoulection.admin.survey.application.dto.SurveyFeatureResult;
import com.seoulection.admin.survey.application.dto.SurveyOptionResult;
import com.seoulection.admin.survey.application.dto.SurveyQuestionResult;
import com.seoulection.admin.survey.domain.entity.SurveyOption;
import com.seoulection.admin.survey.domain.entity.SurveyQuestion;
import com.seoulection.admin.survey.domain.entity.SurveyQuestionRole;
import com.seoulection.admin.survey.domain.repository.SurveyOptionRepository;
import com.seoulection.admin.survey.domain.repository.SurveyQuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 설문 문항·선택지 관리 유스케이스.
 *
 * <p>여기서 하는 편집은 <b>즉시 사용자에게 반영된다</b> — api-server가 매 제출·조회마다 이 테이블을 읽기 때문에
 * 배포도 캐시 무효화도 필요 없다. 뒤집어 말하면 실수도 즉시 반영된다. 지표 매핑(V30)도 마찬가지로
 * api-server의 리졸버가 호출마다 다시 읽는다.
 */
@Service
public class SurveyAdminService {

    private final SurveyQuestionRepository questionRepository;
    private final SurveyOptionRepository optionRepository;
    private final SkinScorePolicyJpaRepository policyRepository;

    public SurveyAdminService(SurveyQuestionRepository questionRepository,
                              SurveyOptionRepository optionRepository,
                              SkinScorePolicyJpaRepository policyRepository) {
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
        this.policyRepository = policyRepository;
    }

    @Transactional(readOnly = true)
    public List<SurveyQuestionResult> getQuestions() {
        Map<String, String> labels = policyLabels();
        return questionRepository.findAllOrdered().stream()
                .map(q -> SurveyQuestionResult.of(q, labels.get(q.getFeatureKey()), findOptions(q.getQuestionKey())))
                .toList();
    }

    /**
     * 문항을 묶을 수 있는 지표 목록. {@code skin_score_policy}가 지표 10개의 마스터다 — 지표 추가는 추천 서버의
     * 제품 특징과 함께 바뀌어야 하므로 어드민에서 늘리지 않는다(드롭다운, 자유 입력 아님).
     */
    @Transactional(readOnly = true)
    public List<SurveyFeatureResult> getFeatures() {
        Map<String, Integer> weightSums = questionRepository.findAllOrdered().stream()
                .filter(SurveyQuestion::isActive)
                .filter(SurveyQuestion::scoresFeature)
                .collect(Collectors.groupingBy(SurveyQuestion::getFeatureKey,
                        Collectors.summingInt(SurveyQuestion::getWeight)));
        return policyRepository.findAll().stream()
                .map(SkinScorePolicyJpaEntity::toDomain)
                .sorted(Comparator.comparing(SkinScorePolicy::featureKey))
                .map(p -> new SurveyFeatureResult(p.featureKey(), p.label(),
                        weightSums.getOrDefault(p.featureKey(), 0), p.minAnsweredWeight()))
                .toList();
    }

    /**
     * 문항 추가. {@code questionKey}는 자연 PK라 유일해야 한다 — 중복을 그냥 저장하면 upsert라서
     * 기존 문항을 조용히 덮어써 버린다(제목·순서가 실수로 바뀐다). 그래서 여기서 먼저 걸러 안내한다.
     *
     * @param featureKey 묶을 지표. 비우면 점수에 쓰이지 않는 문항(표시용).
     */
    @Transactional
    public void createQuestion(String questionKey, String title, int sortOrder,
                               String featureKey, int weight, SurveyQuestionRole role) {
        String normalizedKey = questionKey == null ? "" : questionKey.trim().toUpperCase();
        if (questionRepository.existsByKey(normalizedKey)) {
            throw new IllegalArgumentException("이미 있는 문항입니다: " + normalizedKey);
        }
        SurveyQuestion question = SurveyQuestion.create(normalizedKey, title, sortOrder, featureKey, weight, role);
        requireKnownFeature(question.getFeatureKey());
        questionRepository.save(question);
    }

    /** 지표 없이 문항 추가. 옛 호출부 호환 — 표시용(FLAG) 문항이 된다. */
    @Transactional
    public void createQuestion(String questionKey, String title, int sortOrder) {
        createQuestion(questionKey, title, sortOrder, null, SurveyQuestion.DEFAULT_WEIGHT, SurveyQuestionRole.FLAG);
    }

    /**
     * 지표·비중·역할 수정. 저장 즉시 다음 추천 계산부터 반영된다 — 이미 제출된 응답을 다시 읽어 새 비중으로
     * 계산하므로, 비중을 바꾸면 기존 사용자의 설문 점수도 바뀐다.
     */
    @Transactional
    public void updateQuestionMapping(String questionKey, String featureKey, int weight, SurveyQuestionRole role) {
        SurveyQuestion question = getQuestion(questionKey).withMapping(featureKey, weight, role);
        requireKnownFeature(question.getFeatureKey());
        questionRepository.save(question);
    }

    /**
     * 선택지 추가. {@code code}는 문항 안에서 유일해야 한다 — 중복을 허용하면 사용자 응답이 어느 쪽을
     * 가리키는지 알 수 없어진다(DB 유니크 제약도 있지만 여기서 먼저 걸러 안내 문구를 준다).
     */
    @Transactional
    public void createOption(String questionKey, String code, String label, Integer value,
                             int sortOrder, boolean exclusive, Integer capsFeatureAt, boolean referDermatologist) {
        String normalizedCode = code == null ? "" : code.trim().toUpperCase();
        if (optionRepository.existsByQuestionKeyAndCode(questionKey, normalizedCode)) {
            throw new IllegalArgumentException("이미 있는 코드입니다: " + normalizedCode);
        }
        optionRepository.save(SurveyOption.create(questionKey, normalizedCode, label, value, sortOrder, exclusive,
                capsFeatureAt, referDermatologist));
    }

    /** 상한·진료 안내 없는 선택지 추가. 옛 호출부 호환. */
    @Transactional
    public void createOption(String questionKey, String code, String label, Integer value,
                             int sortOrder, boolean exclusive) {
        createOption(questionKey, code, label, value, sortOrder, exclusive, null, false);
    }

    /** 문구·점수·순서·단독선택·상한·진료 안내 수정. {@code code}는 대상이 아니다(도메인이 막는다). */
    @Transactional
    public void updateOption(Long optionId, String label, Integer value, int sortOrder, boolean exclusive,
                             Integer capsFeatureAt, boolean referDermatologist) {
        SurveyOption option = getOption(optionId);
        optionRepository.save(option.withDetails(label, value, sortOrder, exclusive, capsFeatureAt, referDermatologist));
    }

    /** 상한·진료 안내는 그대로 두는 수정. 옛 호출부 호환. */
    @Transactional
    public void updateOption(Long optionId, String label, Integer value, int sortOrder, boolean exclusive) {
        SurveyOption option = getOption(optionId);
        optionRepository.save(option.withDetails(label, value, sortOrder, exclusive));
    }

    /**
     * 노출/숨김 전환. <b>이것이 삭제다</b> — 행을 지우지 않는 이유는 이미 제출된 응답이 이 코드를 참조하고
     * 있어서, 지우면 과거 응답의 문구를 되찾을 수 없기 때문이다. 되살리기도 같은 경로다.
     */
    @Transactional
    public void changeOptionActive(Long optionId, boolean active) {
        SurveyOption option = getOption(optionId);
        optionRepository.save(option.withActive(active));
    }

    @Transactional
    public void updateQuestionTitle(String questionKey, String title) {
        SurveyQuestion question = getQuestion(questionKey);
        questionRepository.save(question.withTitle(title));
    }

    /**
     * 문항 노출/숨김 전환. <b>이것이 문항 삭제다</b> — 이미 이 문항으로 답한 응답이 있어, 행을 지우면
     * 그 응답이 가리키는 문항 문구를 되찾을 수 없다({@link #changeOptionActive} 참조). 되살리기도 같은 경로다.
     *
     * <p>숨겨도 이미 제출된 응답과 그 선택지들은 그대로 남는다 — {@code GET /survey/questions}에서만 빠진다.
     * api-server는 같은 지표의 <b>활성</b> 문항에 답이 없는 사용자에 한해 숨긴 문항의 답으로 계속 계산한다.
     */
    @Transactional
    public void changeQuestionActive(String questionKey, boolean active) {
        SurveyQuestion question = getQuestion(questionKey);
        questionRepository.save(question.withActive(active));
    }

    /**
     * 운영 DB에서는 FK가 막지만, 제약 위반은 롤백 시점에 알 수 없는 예외로 터진다. 여기서 먼저 걸러
     * 화면에 사유를 보여준다. 테스트 스키마(어드민 매핑으로 생성)에는 FK가 없어서 이 검사가 유일한 방어다.
     */
    private void requireKnownFeature(String featureKey) {
        if (featureKey != null && !policyRepository.existsById(featureKey)) {
            throw new IllegalArgumentException("없는 피부 지표입니다: " + featureKey);
        }
    }

    private Map<String, String> policyLabels() {
        return policyRepository.findAll().stream()
                .map(SkinScorePolicyJpaEntity::toDomain)
                .collect(Collectors.toMap(SkinScorePolicy::featureKey, SkinScorePolicy::label, (a, b) -> a));
    }

    private SurveyQuestion getQuestion(String questionKey) {
        return questionRepository.findByKey(questionKey)
                .orElseThrow(() -> new IllegalArgumentException("없는 문항입니다: " + questionKey));
    }

    private List<SurveyOptionResult> findOptions(String questionKey) {
        return optionRepository.findByQuestionKey(questionKey).stream()
                .map(SurveyOptionResult::from)
                .toList();
    }

    private SurveyOption getOption(Long optionId) {
        return optionRepository.findById(optionId)
                .orElseThrow(() -> new IllegalArgumentException("없는 선택지입니다: " + optionId));
    }
}
