package com.seoulection.admin.product.functional.domain;

/**
 * 점수가 매겨진 후보 한 건. 화면의 후보 표가 이 값을 그대로 그린다.
 *
 * @param numericMatch 숫자 토큰이 일치하는가. false면 점수와 무관하게 자동 확정하지 않는다.
 * @param brandMatch   등록명이 브랜드로 시작하거나 업체명이 브랜드 등록 업체와 같은가.
 */
public record MfdsCandidate(
        MfdsItem item,
        double score,
        ClaimReading claims,
        boolean numericMatch,
        boolean brandMatch
) {
    public static MfdsCandidate of(MfdsItem item, String queryName, String brand, String brandEntpName) {
        boolean entpMatch = brandEntpName != null && brandEntpName.equals(item.entpName());
        return new MfdsCandidate(
                item,
                ItemName.similarity(queryName, item.itemName()),
                FunctionalClaims.read(item),
                ItemName.numericTokensMatch(queryName, item.itemName()),
                entpMatch || ItemName.startsWithBrand(item.itemName(), brand));
    }

    /** 점수만으로 자동 확정해도 되는 후보인가. LLM 판정과는 별개로 항상 함께 본다. */
    public boolean confirmable(double autoThreshold) {
        return score >= autoThreshold && numericMatch && brandMatch && claims.autoConfirmable();
    }

    /** 자동 확정을 막은 이유. 화면에 그대로 보여 준다. */
    public String blockReason() {
        if (!numericMatch) {
            return "제품명 숫자가 다릅니다(" + ItemName.numericTokens(item.itemName()) + ") — 다른 제품일 수 있습니다";
        }
        if (!brandMatch) {
            return "등록 업체(" + item.entpName() + ")가 이 브랜드와 이어지지 않습니다";
        }
        if (!claims.autoConfirmable()) {
            return claims.reason();
        }
        return "";
    }
}
