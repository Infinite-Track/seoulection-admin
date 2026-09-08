package com.seoulection.admin.product.application.dto;

/**
 * 특성 키의 정의. 화면이 "무엇을 입력할 수 있는가"를 이 목록으로 그린다.
 *
 * <p>{@code valueType} 이 NUMERIC/NUMERIC_RANGE 면 숫자 칸을, 그 외에는 텍스트 칸을 보여준다.
 */
public record PropertyDefinitionResult(String propertyKey, String displayNameKo, String valueType,
                                       String valueUnit, String description) {

    public boolean numeric() {
        return valueType != null && valueType.toUpperCase().startsWith("NUMERIC");
    }
}
