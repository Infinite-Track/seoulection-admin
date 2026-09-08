package com.seoulection.admin.product.functional.domain;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 안전나라 등록명(ITEM_NAME)과 우리 제품명을 견주기 위한 문자열 도구.
 *
 * <p>등록명은 공백·기호가 전부 빠진 한 덩어리다("구달청귤비타씨잡티세럼"). 그래서 비교
 * 전에 양쪽을 같은 규칙으로 눌러 놓아야 한다.
 */
public final class ItemName {

    /** 대괄호·소괄호 안은 통째로 버린다 — "[SPF50+/PA++++]", "(보)" 같은 꼬리표다. */
    private static final Pattern BRACKET = Pattern.compile("[\\[(].*?[\\])]");
    private static final Pattern NON_ALNUM = Pattern.compile("[^가-힣a-zA-Z0-9]");
    private static final Pattern DIGITS = Pattern.compile("\\d+");

    private ItemName() {
    }

    /** 비교용 정규형. 괄호 제거 → 한글/영문/숫자만 남김 → 소문자. */
    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String stripped = BRACKET.matcher(value).replaceAll("");
        return NON_ALNUM.matcher(stripped).replaceAll("").toLowerCase();
    }

    /**
     * 0~1 유사도. 최장 공통 부분수열 기반이다 — 등록명은 중간에 단어가 끼어드는 경우가
     * 많아("딥클린<b>아크네</b>포밍클렌저") 편집거리보다 부분수열이 실제와 잘 맞는다.
     */
    public static double similarity(String left, String right) {
        String a = normalize(left);
        String b = normalize(right);
        if (a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }
        return 2.0 * longestCommonSubsequence(a, b) / (a.length() + b.length());
    }

    /**
     * 이름 안의 숫자 토큰. <b>자동 확정을 막는 하드 룰</b>의 근거다.
     *
     * <p>실측에서 "닥터디퍼런트 311 모이스처라이저"가 등록명 "닥터디퍼런트131모이스처라이저"와
     * 0.94로 붙었다. 숫자 한 자리가 다른 <b>완전히 다른 제품</b>인데 유사도로는 걸러지지
     * 않는다. 그래서 숫자 집합이 다르면 점수가 아무리 높아도 자동 확정하지 않는다.
     */
    public static Set<String> numericTokens(String value) {
        Set<String> tokens = new LinkedHashSet<>();
        Matcher matcher = DIGITS.matcher(normalize(value));
        while (matcher.find()) {
            tokens.add(matcher.group());
        }
        return tokens;
    }

    /** 한쪽에만 있는 숫자가 있으면 false. 양쪽 다 숫자가 없으면 true. */
    public static boolean numericTokensMatch(String left, String right) {
        return numericTokens(left).equals(numericTokens(right));
    }

    public static boolean startsWithBrand(String itemName, String brand) {
        String normalizedBrand = normalize(brand);
        return !normalizedBrand.isEmpty() && normalize(itemName).startsWith(normalizedBrand);
    }

    private static int longestCommonSubsequence(String a, String b) {
        int[] previous = new int[b.length() + 1];
        int[] current = new int[b.length() + 1];
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                current[j] = a.charAt(i - 1) == b.charAt(j - 1)
                        ? previous[j - 1] + 1
                        : Math.max(previous[j], current[j - 1]);
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[b.length()];
    }
}
