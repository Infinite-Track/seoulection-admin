package com.seoulection.admin.survey.application.service;

import com.seoulection.admin.survey.application.dto.SurveyEvidenceResult;
import com.seoulection.admin.survey.infrastructure.entity.SurveyQuestionEvidenceJpaEntity;
import com.seoulection.admin.survey.infrastructure.repository.SurveyQuestionEvidenceJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 설문 문항의 근거 관리.
 *
 * <p>설문이 "근거 기반" 이어야 한다는 요구에서 나왔다. 문항 하나에 근거 여러 개가 붙는다 —
 * 논문 하나로 시작해도 나중에 가이드라인·임상 자료가 붙기 때문이다.
 */
@Service
public class SurveyEvidenceService {

    /** 마이그레이션의 CHECK 제약과 같은 집합. 여기서 먼저 걸러 DB 오류가 500 으로 새지 않게 한다. */
    private static final Set<String> SOURCE_TYPES = Set.of("PAPER", "GUIDELINE", "CLINICAL", "ARTICLE", "INTERNAL");

    private final SurveyQuestionEvidenceJpaRepository repository;

    public SurveyEvidenceService(SurveyQuestionEvidenceJpaRepository repository) {
        this.repository = repository;
    }

    /** 문항 키 → 근거 목록. 문항마다 쿼리를 쏘지 않으려고 한 번에 읽어 나눈다. */
    public Map<String, List<SurveyEvidenceResult>> byQuestion() {
        return repository.findAllByOrderByQuestionKeyAscSortOrderAscIdAsc().stream()
                .map(SurveyEvidenceResult::from)
                .collect(Collectors.groupingBy(SurveyEvidenceResult::questionKey,
                        LinkedHashMap::new, Collectors.toList()));
    }

    @Transactional
    public void add(String questionKey, String title, String rationale, String url,
                    String sourceType, Integer sortOrder) {
        require(questionKey, "문항을 선택해 주세요.");
        require(title, "근거 제목을 입력해 주세요.");
        // 링크만 모으면 "왜 묻는지"는 여전히 사람 머릿속에 남는다. 요약을 필수로 받는 이유다.
        require(rationale, "이 문항을 왜 묻는지 적어 주세요.");
        repository.save(new SurveyQuestionEvidenceJpaEntity(questionKey.trim(), title.trim(),
                rationale.trim(), blank(url), normalizeSource(sourceType), sortOrder));
    }

    @Transactional
    public void update(Long id, String title, String rationale, String url,
                       String sourceType, Integer sortOrder) {
        require(title, "근거 제목을 입력해 주세요.");
        require(rationale, "이 문항을 왜 묻는지 적어 주세요.");
        SurveyQuestionEvidenceJpaEntity entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("근거를 찾을 수 없습니다."));
        entity.update(title.trim(), rationale.trim(), blank(url), normalizeSource(sourceType), sortOrder);
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    /**
     * 알 수 없는 종류는 거부하지 않고 ARTICLE 로 흘린다.
     *
     * <p>화면의 select 로만 들어오는 값이라 잘못된 값은 사실상 오지 않는다. 그런데 거부하면
     * 근거 입력 자체가 막히고, 종류는 신뢰도 표시용이라 틀려도 손실이 작다 — 근거를 못 남기는
     * 쪽이 더 나쁘다.
     */
    private String normalizeSource(String value) {
        if (value == null) return "ARTICLE";
        String upper = value.trim().toUpperCase();
        return SOURCE_TYPES.contains(upper) ? upper : "ARTICLE";
    }

    private void require(String value, String message) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
    }

    private String blank(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
