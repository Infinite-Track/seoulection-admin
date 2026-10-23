package com.seoulection.admin.demo.application;

import com.seoulection.admin.demo.infrastructure.DemoFaceProfileJpaRepository;
import com.seoulection.admin.demo.infrastructure.DemoFaceProfileScoreJpaRepository;
import com.seoulection.admin.demo.infrastructure.DemoFaceProfileJpaEntity;
import com.seoulection.admin.demo.infrastructure.DemoFaceProfileScoreJpaEntity;
import com.seoulection.admin.skinpolicy.domain.SkinScorePolicy;
import com.seoulection.admin.skinpolicy.infrastructure.SkinScorePolicyJpaEntity;
import com.seoulection.admin.skinpolicy.infrastructure.SkinScorePolicyJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 데모 얼굴 관리({@code demo_face_profile}·{@code demo_face_profile_score}, api-server V37).
 *
 * <p>/start 첫 화면의 얼굴 카드와, 얼굴마다 지표 10개 예시 점수(0~1, 높을수록 좋음). 저장 즉시 다음 데모 추천부터
 * 반영된다. 점수는 분석 결과가 아니라 운영자가 정한 값이고, 사용자 화면에는 "체험용"이라고 표시된다.
 */
@Service
public class DemoFaceService {

    private static final Pattern CODE = Pattern.compile("^[A-Z][A-Z0-9_]{0,31}$");
    /** 점수가 없는 지표를 api-server 가 이 값으로 본다(Good 경계). 화면 기본값도 같게. */
    public static final double DEFAULT_SCORE = 0.75;

    public record Profile(String code, String label, String imageUrl, int sortOrder, boolean active,
                          Map<String, Double> scores) {
        public double scoreOf(String featureKey) {
            return scores.getOrDefault(featureKey, DEFAULT_SCORE);
        }
    }

    private final DemoFaceProfileJpaRepository profiles;
    private final DemoFaceProfileScoreJpaRepository scores;
    private final SkinScorePolicyJpaRepository policyRepository;

    public DemoFaceService(DemoFaceProfileJpaRepository profiles, DemoFaceProfileScoreJpaRepository scores,
                           SkinScorePolicyJpaRepository policyRepository) {
        this.profiles = profiles;
        this.scores = scores;
        this.policyRepository = policyRepository;
    }

    @Transactional(readOnly = true)
    public List<SkinScorePolicy> features() {
        return policyRepository.findAll().stream().map(SkinScorePolicyJpaEntity::toDomain)
                .sorted(Comparator.comparing(SkinScorePolicy::featureKey)).toList();
    }

    /** 비활성 포함 — 관리자는 숨긴 얼굴도 보고 되살린다. */
    @Transactional(readOnly = true)
    public List<Profile> list() {
        Map<String, Map<String, Double>> byCode = new HashMap<>();
        scores.findAllByOrderByCodeAscFeatureKeyAsc().forEach(s ->
                byCode.computeIfAbsent(s.getCode(), c -> new LinkedHashMap<>()).put(s.getFeatureKey(), s.getScore()));
        return profiles.findAllByOrderBySortOrderAscCodeAsc().stream()
                .map(p -> new Profile(p.getCode(), p.getLabel(), p.getImageUrl(), p.getSortOrder(), p.isActive(),
                        Map.copyOf(byCode.getOrDefault(p.getCode(), Map.of()))))
                .toList();
    }

    /** 새 얼굴. 코드는 대문자 스네이크, 중복이면 거부(upsert 로 덮어쓰지 않게). 점수는 전부 기본값으로 시작. */
    @Transactional
    public void create(String code, String label, String imageUrl, int sortOrder) {
        String normalized = normalizeCode(code);
        if (profiles.existsById(normalized)) {
            throw new IllegalArgumentException("이미 있는 데모 얼굴입니다: " + normalized);
        }
        requireText(label, "이름");
        requireText(imageUrl, "이미지 URL");
        profiles.save(new DemoFaceProfileJpaEntity(normalized, label.trim(), imageUrl.trim(), sortOrder, true));
        scores.saveAll(policyRepository.findAll().stream()
                .map(p -> new DemoFaceProfileScoreJpaEntity(normalized, p.toDomain().featureKey(), DEFAULT_SCORE))
                .toList());
    }

    /** 이름·이미지·순서·노출 + 지표 점수(0~1)를 한 번에 고친다. 코드는 못 바꾼다(사용자 요청이 코드를 든다). */
    @Transactional
    public void update(String code, String label, String imageUrl, int sortOrder, boolean active,
                       Map<String, Double> newScores) {
        DemoFaceProfileJpaEntity current = profiles.findById(code)
                .orElseThrow(() -> new IllegalArgumentException("없는 데모 얼굴입니다: " + code));
        requireText(label, "이름");
        requireText(imageUrl, "이미지 URL");
        var known = policyRepository.findAll().stream().map(p -> p.toDomain().featureKey()).toList();
        for (var e : newScores.entrySet()) {
            if (!known.contains(e.getKey())) {
                throw new IllegalArgumentException("없는 피부 지표입니다: " + e.getKey());
            }
            if (e.getValue() == null || e.getValue() < 0 || e.getValue() > 1) {
                throw new IllegalArgumentException(e.getKey() + " 점수는 0~1이어야 합니다.");
            }
        }
        profiles.save(new DemoFaceProfileJpaEntity(current.getCode(), label.trim(), imageUrl.trim(), sortOrder, active));
        scores.deleteByCode(code);
        scores.flush();
        scores.saveAll(newScores.entrySet().stream()
                .map(e -> new DemoFaceProfileScoreJpaEntity(code, e.getKey(), e.getValue())).toList());
    }

    private static String normalizeCode(String code) {
        String normalized = code == null ? "" : code.trim().toUpperCase();
        if (!CODE.matcher(normalized).matches()) {
            throw new IllegalArgumentException("코드는 영문 대문자·숫자·밑줄 32자 이내여야 합니다(예: DEMO_WRINKLES).");
        }
        return normalized;
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + "을(를) 입력해 주세요.");
        }
    }
}
