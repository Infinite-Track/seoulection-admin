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
public record ProductIngredientProperty(String propertyKey, String displayNameKo, String valueType,
                                        String valueText, BigDecimal valueMin, BigDecimal valueMax,
                                        String valueUnit, String notes) {

    /** 폼에서 올라온 값. 표시용 이름과 타입은 정의에서 오는 것이라 여기서는 모른다. */
    public ProductIngredientProperty(String propertyKey, String valueText, BigDecimal valueMin,
                                     BigDecimal valueMax, String valueUnit, String notes) {
        this(propertyKey, null, null, valueText, valueMin, valueMax, valueUnit, notes);
    }

    /**
     * 숫자형인가. 화면이 최소·최대 두 칸을 그릴지 텍스트 한 칸을 그릴지 이걸로 정한다.
     *
     * <p>저장된 특성에도 타입이 붙어 있어야 하는 이유: 화면에서 정의 목록을 다시 뒤져
     * 짝을 찾는 식은 표현식이 길어지고 실제로 한 번 깨진 적이 있다. 조회 쿼리가 정의를
     * 이미 조인하고 있으니 거기서 같이 가져온다.
     */
    public boolean numeric() {
        if (valueType == null) {
            return valueMin != null || valueMax != null;
        }
        String upper = valueType.toUpperCase();
        return upper.startsWith("NUMERIC") || upper.startsWith("NUMBER");
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
