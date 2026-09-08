package com.seoulection.admin.product.functional.domain;

import com.seoulection.admin.product.domain.enums.ProductFunctionalCategory;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 안전나라 응답 → {@link ProductFunctionalCategory} 변환. <b>유형의 유일한 출처</b>다.
 *
 * <p>LLM은 여기에 관여하지 않는다. 검색어를 만들고 후보를 고르는 데까지만 쓰고, "이 제품이
 * 무슨 기능성인가"는 반드시 API 응답 필드에서만 나온다 — 규제 정보를 모델이 지어내지
 * 못하게 하는 경계다.
 *
 * <p>{@code COSMETIC_TARGET_FLAG_NAME}(제10조 제1항 제○호)을 쓰지 않는 이유: 그건 효능이
 * 아니라 보고 근거 구분이다. 실측하면 제1호 안에 미백·주름·염모·탈색이 전부 섞여 있다.
 */
public final class FunctionalClaims {

    private FunctionalClaims() {
    }

    /** 우리 분류 밖이지만 명백히 기능성인 효능. 이게 걸리면 자동 확정하지 않는다. */
    private static final List<String> OUT_OF_SCOPE_KEYWORDS = List.of("염모", "탈색", "탈염", "제모", "탈모", "산화제");

    public static ClaimReading read(MfdsItem item) {
        String ee = item.eeName() == null ? "" : item.eeName();

        if (!ee.isBlank()) {
            Set<ProductFunctionalCategory> categories = new LinkedHashSet<>();
            if (ee.contains("미백")) {
                categories.add(ProductFunctionalCategory.WHITENING);
            }
            if (ee.contains("주름")) {
                categories.add(ProductFunctionalCategory.WRINKLE_IMPROVEMENT);
            }
            if (ee.contains("자외선") || ee.contains("자외선차단")) {
                categories.add(ProductFunctionalCategory.UV_PROTECTION);
            }
            if (ee.contains("여드름")) {
                categories.add(ProductFunctionalCategory.ACNE_RELIEF);
            }
            if (ee.contains("아토피") || ee.contains("피부장벽")) {
                categories.add(ProductFunctionalCategory.SKIN_BARRIER_RECOVERY);
            }
            if (ee.contains("튼살")) {
                categories.add(ProductFunctionalCategory.STRETCH_MARKS);
            }
            // SPF/PA가 붙어 있으면 문구에 '자외선'이 없어도 자외선 차단이다.
            if (item.hasUvMeasurement()) {
                categories.add(ProductFunctionalCategory.UV_PROTECTION);
            }
            if (!categories.isEmpty()) {
                return new ClaimReading(List.copyOf(categories), false, true);
            }
            boolean outOfScope = OUT_OF_SCOPE_KEYWORDS.stream().anyMatch(ee::contains);
            return ClaimReading.none(outOfScope, true);
        }

        // EE_NAME이 비어 있는 행이 실제로 많다. SPF/PA가 있으면 선크림 보고 건의 전형이다.
        if (item.hasUvMeasurement()) {
            return new ClaimReading(List.of(ProductFunctionalCategory.UV_PROTECTION), false, true);
        }

        // 등록은 돼 있는데 유형을 알 수 없는 경우 — 여기서 멈추고 사람에게 넘긴다.
        return ClaimReading.none(false, false);
    }
}
