package com.seoulection.admin.skinpolicy.domain;

/**
 * 설문 답으로 피부타입을 계산하는 규칙 — {@code survey_skin_type_rule} 한 행(api-server V34).
 *
 * <p>점수는 0~100, 높을수록 번들거림·건조감이 적다. T존·볼이 모두 {@code oilyMax} 이하면 지성, T존만 기름지고
 * 볼이 {@code clearMin} 이상이면 복합성, 둘 다 {@code clearMin} 이상이면 건조감이 {@code dryWaterMax} 미만일 때 건성,
 * 아니면 일반. 그 밖의 조합은 자기 선언(SKIN_TYPE_CHECK)으로 돌아간다. {@code preferSelfDeclared}를 켜면 자기 선언이
 * 있을 때 그것을 먼저 쓴다.
 */
public record SkinTypeRule(String tZoneQuestionKey, String cheekQuestionKey, int oilyMax, int clearMin,
                           int dryWaterMax, boolean preferSelfDeclared) {

    public static final SkinTypeRule DEFAULT =
            new SkinTypeRule("OILY_INTENSITY_CORE_V24", "OILY_INTENSITY_SITUATIONAL_V24", 50, 75, 40, false);

    public SkinTypeRule {
        requireKey(tZoneQuestionKey, "T존 문항");
        requireKey(cheekQuestionKey, "볼 문항");
        if (tZoneQuestionKey.equals(cheekQuestionKey)) {
            throw new IllegalArgumentException("T존 문항과 볼 문항은 서로 달라야 합니다.");
        }
        requireRange(oilyMax, "지성 기준");
        requireRange(clearMin, "비지성 기준");
        requireRange(dryWaterMax, "건성 기준");
        if (oilyMax >= clearMin) {
            throw new IllegalArgumentException("지성 기준(" + oilyMax + ")은 비지성 기준(" + clearMin + ")보다 작아야 합니다.");
        }
    }

    private static void requireKey(String key, String name) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException(name + "을 골라 주세요.");
        }
    }

    private static void requireRange(int value, String name) {
        if (value < 0 || value > 100) {
            throw new IllegalArgumentException(name + "은 0~100이어야 합니다.");
        }
    }
}
