package com.seoulection.admin.survey.application.service;

import com.seoulection.admin.survey.application.dto.SurveyOptionResult;
import com.seoulection.admin.survey.application.dto.SurveyQuestionResult;
import com.seoulection.admin.survey.domain.entity.SurveyOption;
import com.seoulection.admin.survey.domain.entity.SurveyQuestion;
import com.seoulection.admin.survey.domain.repository.SurveyOptionRepository;
import com.seoulection.admin.survey.domain.repository.SurveyQuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 설문 문항·선택지 관리 유스케이스.
 *
 * <p>여기서 하는 편집은 <b>즉시 사용자에게 반영된다</b> — api-server가 매 제출·조회마다 이 테이블을 읽기 때문에
 * 배포도 캐시 무효화도 필요 없다. 뒤집어 말하면 실수도 즉시 반영된다.
 */
@Service
public class SurveyAdminService {

    private final SurveyQuestionRepository questionRepository;
    private final SurveyOptionRepository optionRepository;

    public SurveyAdminService(SurveyQuestionRepository questionRepository,
                              SurveyOptionRepository optionRepository) {
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
    }

    @Transactional(readOnly = true)
    public List<SurveyQuestionResult> getQuestions() {
        return questionRepository.findAllOrdered().stream()
                .map(q -> SurveyQuestionResult.of(q, findOptions(q.getQuestionKey())))
                .toList();
    }

    /**
     * 문항 추가. {@code questionKey}는 자연 PK라 유일해야 한다 — 중복을 그냥 저장하면 upsert라서
     * 기존 문항을 조용히 덮어써 버린다(제목·순서가 실수로 바뀐다). 그래서 여기서 먼저 걸러 안내한다.
     */
    @Transactional
    public void createQuestion(String questionKey, String title, int sortOrder) {
        String normalizedKey = questionKey == null ? "" : questionKey.trim().toUpperCase();
        if (questionRepository.existsByKey(normalizedKey)) {
            throw new IllegalArgumentException("이미 있는 문항입니다: " + normalizedKey);
        }
        questionRepository.save(SurveyQuestion.create(normalizedKey, title, sortOrder));
    }

    /**
     * 선택지 추가. {@code code}는 문항 안에서 유일해야 한다 — 중복을 허용하면 사용자 응답이 어느 쪽을
     * 가리키는지 알 수 없어진다(DB 유니크 제약도 있지만 여기서 먼저 걸러 안내 문구를 준다).
     */
    @Transactional
    public void createOption(String questionKey, String code, String label, Integer value,
                             int sortOrder, boolean exclusive) {
        String normalizedCode = code == null ? "" : code.trim().toUpperCase();
        if (optionRepository.existsByQuestionKeyAndCode(questionKey, normalizedCode)) {
            throw new IllegalArgumentException("이미 있는 코드입니다: " + normalizedCode);
        }
        optionRepository.save(
                SurveyOption.create(questionKey, normalizedCode, label, value, sortOrder, exclusive));
    }

    /** 문구·점수·순서·단독선택 수정. {@code code}는 대상이 아니다(도메인이 막는다). */
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
     */
    @Transactional
    public void changeQuestionActive(String questionKey, boolean active) {
        SurveyQuestion question = getQuestion(questionKey);
        questionRepository.save(question.withActive(active));
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
