package com.seoulection.admin.product.functional.domain;

/**
 * 기능성화장품 등록 경로. 법적으로 동등하다 — 심사는 새 처방을 식약처가 심사한 것이고,
 * 보고는 이미 고시된 기준에 맞아 신고만 한 것이다. 어느 쪽에 있든 기능성화장품이다.
 */
public enum MfdsSource {
    REPORT("보고"),
    EXAMINATION("심사");

    private final String displayName;

    MfdsSource(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
