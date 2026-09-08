package com.seoulection.admin.product.application.dto;

import java.math.BigDecimal;

/**
 * 제품 안에서의 성분 특성 한 줄.
 *
 * <p>왜 컬럼이 아니라 이런 목록인가: 담아야 할 축이 순도만이 아니다. "이 제품의 히알루론산은
 * 저분자", "아줄렌 순도 99%" 처럼 성분·제품마다 다른 축이 계속 생긴다. 컬럼으로 두면 축이
 * 하나 늘 때마다 마이그레이션이 필요하다.
 *
 * <p>수치는 {@code valueMin}/{@code valueMax}, 범주형은 {@code valueText} 를 쓴다.
 * 어느 쪽을 쓸지는 {@code property_definition.value_type} 이 정한다.
 */
public record ProductIngredientProperty(String propertyKey, String displayNameKo, String valueText,
                                        BigDecimal valueMin, BigDecimal valueMax, String valueUnit,
                                        String notes) {

    public ProductIngredientProperty(String propertyKey, String valueText, BigDecimal valueMin,
                                     BigDecimal valueMax, String valueUnit, String notes) {
        this(propertyKey, null, valueText, valueMin, valueMax, valueUnit, notes);
    }

    /** 화면에 한 줄로 보여줄 값. 범위면 "1~5 %", 단일값이면 "99 %", 범주형이면 그 문자열. */
    public String display() {
        if (valueText != null && !valueText.isBlank()) return valueText;
        if (valueMin == null && valueMax == null) return "";
        String unit = valueUnit == null ? "" : " " + valueUnit;
        if (valueMin != null && valueMax != null && valueMin.compareTo(valueMax) != 0) {
            return valueMin.stripTrailingZeros().toPlainString() + "~"
                    + valueMax.stripTrailingZeros().toPlainString() + unit;
        }
        BigDecimal single = valueMin != null ? valueMin : valueMax;
        return single.stripTrailingZeros().toPlainString() + unit;
    }
}
