package com.seoulection.admin.product.domain.enums;

import java.util.Arrays;

public enum ProductCategory {
    SUNSCREEN("sunscreens"),
    TONER("toners"),
    TREATMENT("treatments"),
    MOISTURIZER("moisturizers"),
    CLEANSER("cleansers"),
    FACE_MASKS("face masks");

    private final String value;

    ProductCategory(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static ProductCategory from(String value) {
        if (value == null) {
            throw new IllegalArgumentException("category은(는) 필수입니다.");
        }

        return Arrays.stream(values())
                .filter(category -> category.value.equals(value.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 category입니다: " + value));
    }
}
