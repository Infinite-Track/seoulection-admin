package com.seoulection.admin.product.functional.infrastructure;

import com.seoulection.admin.product.domain.enums.ProductFunctionalCategory;
import com.seoulection.admin.product.functional.application.FunctionalScreeningProperties;
import com.seoulection.admin.product.functional.domain.FunctionalClaims;
import com.seoulection.admin.product.functional.domain.ItemName;
import com.seoulection.admin.product.functional.domain.MfdsItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실제 의약품안전나라를 부르는 확인용 테스트. {@code MFDS_SERVICE_KEY}가 있을 때만 돈다.
 *
 * <p>여기 있는 이유: 이 연동에서 깨지는 건 대개 우리 로직이 아니라 <b>키 인코딩과 응답 스키마</b>다.
 * 단위 테스트는 대역을 쓰므로 그 둘을 못 잡는다. 키를 새로 발급했거나 조회가 이상할 때
 * {@code MFDS_SERVICE_KEY=... ./gradlew test --tests '*MfdsCatalogClientLiveTest*'} 로 확인한다.
 */
@EnabledIfEnvironmentVariable(named = "MFDS_SERVICE_KEY", matches = ".+")
class MfdsCatalogClientLiveTest {

    private MfdsCatalogClient client() {
        FunctionalScreeningProperties properties = new FunctionalScreeningProperties();
        properties.getMfds().setServiceKey(System.getenv("MFDS_SERVICE_KEY"));
        return new MfdsCatalogClient(properties);
    }

    @Test
    @DisplayName("브랜드 전수 조회로 제품을 찾고 기능성 유형까지 도출한다")
    void findsProductThroughBrandScan() {
        List<MfdsItem> items = client().searchBrand("구달");
        assertThat(items).isNotEmpty();

        String query = "구달청귤비타씨잡티세럼";
        MfdsItem best = items.stream()
                .max(Comparator.comparingDouble(item -> ItemName.similarity(query, item.itemName())))
                .orElseThrow();

        System.out.println("[브랜드 전수] 구달 " + items.size() + "건, 최고 후보: " + best.itemName()
                + " / " + best.entpName() + " / " + FunctionalClaims.read(best).categories());

        assertThat(ItemName.similarity(query, best.itemName())).isGreaterThan(0.9);
        assertThat(best.entpName()).contains("클리오");
        assertThat(FunctionalClaims.read(best).autoConfirmable()).isTrue();
    }

    @Test
    @DisplayName("효능효과가 비어 있는 선크림도 SPF/PA로 자외선 차단이 도출된다")
    void derivesUvProtectionFromSpf() {
        List<MfdsItem> items = client().searchByItemName("달바워터풀톤업선크림");
        assertThat(items).isNotEmpty();

        MfdsItem sunscreen = items.get(0);
        System.out.println("[선크림] " + sunscreen.itemName() + " / EE=" + sunscreen.eeName()
                + " / SPF=" + sunscreen.spf() + " PA=" + sunscreen.pa()
                + " → " + FunctionalClaims.read(sunscreen).categories());

        assertThat(FunctionalClaims.read(sunscreen).categories()).contains(ProductFunctionalCategory.UV_PROTECTION);
    }

    @Test
    @DisplayName("키가 비어 있으면 빈 목록이 아니라 예외다 — 조용히 '기능성 아님'이 되면 안 된다")
    void missingKeyFailsLoudly() {
        MfdsCatalogClient noKey = new MfdsCatalogClient(new FunctionalScreeningProperties());

        assertThat(org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> noKey.searchByItemName("구달")).getMessage()).contains("서비스 키");
    }
}
