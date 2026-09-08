package com.seoulection.admin.product.application.dto;

/**
 * 특성 키의 정의. 화면이 "무엇을 입력할 수 있는가"를 이 목록으로 그린다.
 *
 * <p>{@code valueType} 이 숫자형이면 최소·최대 두 칸을, 그 외에는 텍스트 한 칸을 보여준다.
 *
 * <p>⚠️ 실제 {@code property_definition.value_type} 값은 {@code NUMBER}/{@code TEXT} 다.
 * 예전 판정은 {@code NUMERIC} 으로 시작하는지만 봐서 <b>모든 정의가 텍스트로 렌더링</b>됐다
 * (순도·분자량에 최소/최대 대신 자유 입력 칸이 떴다). 두 어휘를 모두 받는다.
 */
public record PropertyDefinitionResult(String propertyKey, String displayNameKo, String valueType,
                                       String valueUnit, String description) {

    public boolean numeric() {
        if (valueType == null) {
            return false;
        }
        String upper = valueType.toUpperCase();
        return upper.startsWith("NUMERIC") || upper.startsWith("NUMBER");
    }
}
