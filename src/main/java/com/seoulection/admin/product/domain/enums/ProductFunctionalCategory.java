package com.seoulection.admin.product.domain.enums;

import java.util.Arrays;

/** 식약처 기능성화장품 표시 목적 분류. 성분 효능(ingredient_effect)과는 별도다. */
public enum ProductFunctionalCategory {
    WHITENING("WHITENING", "미백"),
    WRINKLE_IMPROVEMENT("WRINKLE_IMPROVEMENT", "주름 개선"),
    UV_PROTECTION("UV_PROTECTION", "자외선 차단"),
    ACNE_RELIEF("ACNE_RELIEF", "여드름성 피부 완화"),
    SKIN_BARRIER_RECOVERY("SKIN_BARRIER_RECOVERY", "피부장벽 기능 회복"),
    STRETCH_MARKS("STRETCH_MARKS", "튼살 완화");

    private final String code;
    private final String displayName;

    ProductFunctionalCategory(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String code() { return code; }
    public String displayName() { return displayName; }

    public static ProductFunctionalCategory from(String value) {
        return Arrays.stream(values())
                .filter(category -> category.code.equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 기능성 분류입니다: " + value));
    }
}
