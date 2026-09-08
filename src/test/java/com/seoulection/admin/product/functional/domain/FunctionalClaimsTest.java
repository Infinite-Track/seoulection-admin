package com.seoulection.admin.product.functional.domain;

import com.seoulection.admin.product.domain.enums.ProductFunctionalCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** 실제 안전나라 응답에서 관찰된 행 모양을 그대로 고정한다. */
class FunctionalClaimsTest {

    private static MfdsItem item(String eeName, String spf, String pa) {
        return new MfdsItem(MfdsSource.REPORT, "테스트제품", "(주)테스트", null, eeName, spf, pa,
                "제10조 제1항 제1호", "20240101", false);
    }

    @Test
    @DisplayName("효능효과 문구 하나에 두 유형이 들어 있으면 둘 다 읽는다")
    void readsMultipleClaimsFromOneSentence() {
        ClaimReading reading = FunctionalClaims.read(
                item("피부의 미백에 도움을 준다. 피부의 주름개선에 도움을 준다.", null, null));

        assertThat(reading.categories()).containsExactlyInAnyOrder(
                ProductFunctionalCategory.WHITENING, ProductFunctionalCategory.WRINKLE_IMPROVEMENT);
        assertThat(reading.autoConfirmable()).isTrue();
    }

    @Test
    @DisplayName("효능효과가 비어 있어도 SPF/PA가 있으면 자외선 차단이다")
    void inferesUvProtectionFromSpf() {
        // 선크림 보고 건의 전형: EE_NAME이 null이고 SPF·PA만 채워져 온다.
        ClaimReading reading = FunctionalClaims.read(item(null, "50+", "4"));

        assertThat(reading.categories()).containsExactly(ProductFunctionalCategory.UV_PROTECTION);
        assertThat(reading.derivable()).isTrue();
    }

    @Test
    @DisplayName("염모는 기능성이지만 우리 분류 밖이라 자동 확정하지 않는다")
    void hairDyeIsOutOfScope() {
        ClaimReading reading = FunctionalClaims.read(item("모발의 염모", null, null));

        assertThat(reading.categories()).isEmpty();
        assertThat(reading.outOfScope()).isTrue();
        assertThat(reading.autoConfirmable()).isFalse();
    }

    @Test
    @DisplayName("효능효과도 SPF도 없으면 도출 불가 — 사람이 봐야 한다")
    void undecidableWithoutAnyEvidence() {
        ClaimReading reading = FunctionalClaims.read(item(null, null, null));

        assertThat(reading.derivable()).isFalse();
        assertThat(reading.autoConfirmable()).isFalse();
    }
}
