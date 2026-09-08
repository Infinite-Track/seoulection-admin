package com.seoulection.admin.product.functional.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ItemNameTest {

    @Test
    @DisplayName("공백·괄호를 지우면 등록명과 같은 모양이 된다")
    void normalizesToRegistrationShape() {
        assertThat(ItemName.normalize("달바 워터풀 톤업 선크림 [SPF50+/PA++++]"))
                .isEqualTo("달바워터풀톤업선크림");
    }

    @Test
    @DisplayName("중간에 단어가 끼어들어도 유사도가 살아 있다")
    void toleratesInsertedWords() {
        // 실제 사례: 뉴트로지나딥클린포밍클렌저 ↔ 뉴트로지나딥클린아크네포밍클렌저(보)
        assertThat(ItemName.similarity("뉴트로지나 딥클린 포밍 클렌저", "뉴트로지나딥클린아크네포밍클렌저(보)"))
                .isGreaterThan(0.85);
    }

    @Test
    @DisplayName("숫자 한 자리가 다르면 유사도가 높아도 다른 제품으로 표시된다")
    void numericTokensGuardAgainstLookalikes() {
        // 이 두 이름의 유사도는 0.94다 — 점수만 보면 자동 확정될 뻔한 실제 사례.
        String ours = "닥터디퍼런트 311 모이스처라이저";
        String theirs = "닥터디퍼런트131모이스처라이저";

        assertThat(ItemName.similarity(ours, theirs)).isGreaterThan(0.9);
        assertThat(ItemName.numericTokensMatch(ours, theirs)).isFalse();
    }

    @Test
    @DisplayName("양쪽 다 숫자가 없으면 숫자 규칙은 통과다")
    void numericRulePassesWhenNoDigits() {
        assertThat(ItemName.numericTokensMatch("구달 청귤 세럼", "구달청귤비타씨세럼")).isTrue();
    }
}
