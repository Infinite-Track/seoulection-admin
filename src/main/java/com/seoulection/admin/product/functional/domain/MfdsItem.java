package com.seoulection.admin.product.functional.domain;

/**
 * 안전나라 응답 한 행에서 우리가 쓰는 것만 추린 값.
 *
 * <p>필드가 왜 이것들인지: {@code itemName}·{@code entpName}은 매칭에, {@code eeName}·
 * {@code spf}·{@code pa}는 기능성 유형 도출에, {@code canceled}는 후보 제외에 쓴다.
 * {@code targetFlagName}(제10조 제1항 제○호)은 <b>효능이 아니라 보고 근거 구분</b>이라
 * 유형 판정에는 쓰지 않고 화면에 근거로만 보여 준다.
 */
public record MfdsItem(
        MfdsSource source,
        String itemName,
        String entpName,
        String eeCode,
        String eeName,
        String spf,
        String pa,
        String targetFlagName,
        String reportDate,
        boolean canceled
) {
    public boolean hasUvMeasurement() {
        return notBlank(spf) || notBlank(pa);
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
