package com.seoulection.admin.product.functional.domain;

import com.seoulection.admin.product.domain.enums.ProductFunctionalCategory;

import java.time.Instant;
import java.util.List;

/**
 * 제품 한 건의 자동 판정 결과. {@code functional_screenings} 컬렉션에 제품당 1건 남는다.
 *
 * <p>{@code ProductStatus}에 새 상태를 만들지 않는 이유: 지금 설계에서 status가 답하는
 * 질문은 "지금 누가 움직여야 하는가"뿐이고, 판정의 내막(후보가 무엇이었고 왜 자동 확정을
 * 못 했는지)은 상태값으로 표현할 성질이 아니다. status는 그대로 두고 내막만 여기 담는다.
 *
 * @param brandRegistryCount 브랜드 전수 조회 건수. 0이면 "이 브랜드는 기능성 등록 자체가
 *                           없다"는 뜻이라 기능성 아님의 근거가 된다.
 * @param engineVersion      판정 규칙 버전. 규칙을 고쳤을 때 재판정 대상을 고르는 열쇠다.
 */
public record FunctionalScreening(
        String productId,
        ScreeningOutcome outcome,
        List<ProductFunctionalCategory> claims,
        List<MfdsCandidate> candidates,
        int selectedIndex,
        String confidence,
        String reason,
        long brandRegistryCount,
        String decidedBy,
        String engineVersion,
        Instant screenedAt
) {

    public static final String ENGINE_VERSION = "2026-09-08";
    public static final String DECIDED_BY_AUTO = "AUTO";
    public static final String DECIDED_BY_ADMIN = "ADMIN";

    public static FunctionalScreening failed(String productId, String reason) {
        return new FunctionalScreening(productId, ScreeningOutcome.FAILED, List.of(), List.of(), -1,
                "NONE", reason, 0, DECIDED_BY_AUTO, ENGINE_VERSION, Instant.now());
    }

    public MfdsCandidate selected() {
        return selectedIndex < 0 || selectedIndex >= candidates.size() ? null : candidates.get(selectedIndex);
    }

    public boolean hasCandidates() {
        return !candidates.isEmpty();
    }
}
