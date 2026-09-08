package com.seoulection.admin.product.functional.domain;

/**
 * 자동 판정의 결말. 이 값이 "사람이 봐야 하는가"를 정한다.
 *
 * <p>{@link #FAILED}를 {@link #NOT_MATCHED}와 나눠 두는 이유: API 오류로 못 본 것과 조회해
 * 봤는데 없는 것은 전혀 다른 사실이다. 실패를 "기능성 아님"으로 접으면 규제 정보가 조용히
 * 틀어진다.
 */
public enum ScreeningOutcome {

    AUTO_CONFIRMED("자동 확정", "success", false),
    AUTO_NONE("자동 확정(기능성 아님)", "success", false),
    NEEDS_REVIEW("확인 필요", "warning", true),
    NOT_MATCHED("검색 결과 없음", "warning", true),
    FAILED("조회 실패", "danger", true);

    private final String displayName;
    private final String tone;
    private final boolean requiresAdmin;

    ScreeningOutcome(String displayName, String tone, boolean requiresAdmin) {
        this.displayName = displayName;
        this.tone = tone;
        this.requiresAdmin = requiresAdmin;
    }

    public String displayName() { return displayName; }
    public String tone() { return tone; }
    public boolean requiresAdmin() { return requiresAdmin; }
    public boolean decided() { return this == AUTO_CONFIRMED || this == AUTO_NONE; }
}
