package com.seoulection.admin.skinpolicy.application;

import com.seoulection.admin.skinpolicy.domain.SkinScorePolicy;
import com.seoulection.admin.skinpolicy.infrastructure.SkinAnalyzerFeatureJpaEntity;
import com.seoulection.admin.skinpolicy.infrastructure.SkinAnalyzerFeatureJpaRepository;
import com.seoulection.admin.skinpolicy.infrastructure.SkinScorePolicyJpaEntity;
import com.seoulection.admin.skinpolicy.infrastructure.SkinScorePolicyJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 분석기별 "사진만으로 믿어도 되는 지표" 관리({@code skin_analyzer_feature}, api-server V35).
 *
 * <p>api-server 는 지금 켜진 분석기({@code skin.analyzer}) 이름의 행을 호출마다 읽는다 — 저장 즉시 사진 리포트 필터와
 * 문항 조회의 {@code photoCovered} 에 반영된다. 어떤 분석기가 켜져 있는지는 서버 환경변수라 어드민은 모른다.
 */
@Service
public class SkinAnalyzerFeatureService {

    /** api-server 의 skin.analyzer 값과 같은 꼴 — 소문자·숫자·밑줄·하이픈. */
    private static final Pattern ANALYZER_NAME = Pattern.compile("^[a-z][a-z0-9_-]{0,31}$");

    /** 행이 하나도 없는 새 DB 에서도 화면에 보여줄 분석기. api-server 코드의 구현체 이름과 같다. */
    private static final List<String> KNOWN_ANALYZERS = List.of("ailabtools", "mock");

    public record AnalyzerFeatures(String analyzer, Set<String> featureKeys) {
    }

    private final SkinAnalyzerFeatureJpaRepository repository;
    private final SkinScorePolicyJpaRepository policyRepository;

    public SkinAnalyzerFeatureService(SkinAnalyzerFeatureJpaRepository repository,
                                      SkinScorePolicyJpaRepository policyRepository) {
        this.repository = repository;
        this.policyRepository = policyRepository;
    }

    /** 체크 표의 열 — 지표 10개(skin_score_policy). */
    @Transactional(readOnly = true)
    public List<SkinScorePolicy> features() {
        return policyRepository.findAll().stream().map(SkinScorePolicyJpaEntity::toDomain)
                .sorted(Comparator.comparing(SkinScorePolicy::featureKey)).toList();
    }

    /** 체크 표의 행 — 데이터에 있는 분석기 + 코드에 있는 분석기(행이 없어도 보이게). */
    @Transactional(readOnly = true)
    public List<AnalyzerFeatures> analyzers() {
        Map<String, Set<String>> byAnalyzer = new LinkedHashMap<>();
        KNOWN_ANALYZERS.forEach(a -> byAnalyzer.put(a, new LinkedHashSet<>()));
        repository.findAllByOrderByAnalyzerAscFeatureKeyAsc().forEach(row ->
                byAnalyzer.computeIfAbsent(row.getAnalyzer(), a -> new LinkedHashSet<>()).add(row.getFeatureKey()));
        return byAnalyzer.entrySet().stream().map(e -> new AnalyzerFeatures(e.getKey(), e.getValue())).toList();
    }

    /**
     * 한 분석기의 신뢰 지표 집합을 통째로 바꾼다. 전부 해제하면 행이 없어지고 api-server 는 그 분석기 코드의 고정
     * 목록으로 떨어진다(안전장치) — 화면에서 그 점을 안내한다.
     */
    @Transactional
    public void replace(String analyzer, List<String> featureKeys) {
        String name = analyzer == null ? "" : analyzer.trim().toLowerCase();
        if (!ANALYZER_NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("분석기 이름은 소문자·숫자·밑줄·하이픈 32자 이내여야 합니다(api-server 의 skin.analyzer 값).");
        }
        Set<String> known = policyRepository.findAll().stream()
                .map(e -> e.toDomain().featureKey()).collect(java.util.stream.Collectors.toSet());
        List<String> keys = featureKeys == null ? List.of() : featureKeys;
        for (String key : keys) {
            if (!known.contains(key)) {
                throw new IllegalArgumentException("없는 피부 지표입니다: " + key);
            }
        }
        repository.deleteByAnalyzer(name);
        repository.flush();
        repository.saveAll(keys.stream().distinct().map(k -> new SkinAnalyzerFeatureJpaEntity(name, k)).toList());
    }
}
