package com.seoulection.admin.product.functional.application.port;

/**
 * "후보 중 무엇이 같은 제품인가"에 대한 판정.
 *
 * @param index      후보 목록의 인덱스. 같은 제품이 없으면 -1.
 * @param confidence HIGH / MEDIUM / LOW. HIGH일 때만 자동 확정 후보가 된다(그마저도
 *                   숫자 토큰·기능성 도출 규칙을 다시 통과해야 한다).
 */
public record CandidateVerdict(int index, String confidence, String reason) {

    public static final String HIGH = "HIGH";

    public static CandidateVerdict none(String reason) {
        return new CandidateVerdict(-1, "LOW", reason);
    }

    public boolean matched() {
        return index >= 0;
    }

    public boolean high() {
        return HIGH.equalsIgnoreCase(confidence);
    }
}
