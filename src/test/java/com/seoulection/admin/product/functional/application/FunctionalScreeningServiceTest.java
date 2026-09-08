package com.seoulection.admin.product.functional.application;

import com.seoulection.admin.product.application.dto.ProductResult;
import com.seoulection.admin.product.application.service.ProductService;
import com.seoulection.admin.product.domain.enums.ProductFunctionalCategory;
import com.seoulection.admin.product.domain.enums.ProductStatus;
import com.seoulection.admin.product.functional.application.port.CandidateVerdict;
import com.seoulection.admin.product.functional.application.port.FunctionalScreeningRepository;
import com.seoulection.admin.product.functional.application.port.MfdsCatalogPort;
import com.seoulection.admin.product.functional.application.port.ProductNameResolverPort;
import com.seoulection.admin.product.functional.application.port.ScreeningTarget;
import com.seoulection.admin.product.functional.domain.FunctionalScreening;
import com.seoulection.admin.product.functional.domain.MfdsItem;
import com.seoulection.admin.product.functional.domain.MfdsSource;
import com.seoulection.admin.product.functional.domain.ScreeningOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 자동 판정의 경계를 고정한다. 여기 있는 케이스는 전부 실제 안전나라 응답에서 관찰된 모양이다.
 */
class FunctionalScreeningServiceTest {

    private ProductService productService;
    private FakeCatalog catalog;
    private FunctionalScreeningRepository repository;
    private FunctionalScreeningProperties properties;
    private ProductNameResolverPort resolver;

    @BeforeEach
    void setUp() {
        productService = mock(ProductService.class);
        catalog = new FakeCatalog();
        repository = mock(FunctionalScreeningRepository.class);
        properties = new FunctionalScreeningProperties();
        resolver = mock(ProductNameResolverPort.class);
        // (String) 캐스팅을 빼면 List.of(E...) 오버로드가 잡혀 String이 Object[]로 캐스팅된다.
        when(resolver.koreanBrandAliases(anyString())).thenAnswer(call -> List.of((String) call.getArgument(0)));
        when(resolver.registrationNameCandidates(any(), anyString())).thenReturn(List.of());
        when(resolver.judge(any(), anyList())).thenReturn(CandidateVerdict.none("판정 없음"));
    }

    private FunctionalScreeningService service() {
        return new FunctionalScreeningService(productService, catalog, resolver, repository, properties);
    }

    private ProductResult product(String nameKo, String brand, String category) {
        return new ProductResult("p1", null, "Goodal Serum", nameKo, brand, category, null,
                BigDecimal.ZERO, null, null, 0, BigDecimal.ZERO, null, "ADMIN",
                List.of("정제수"), Map.of(), null, List.of(), ProductStatus.INGREDIENTS_ADDED);
    }

    private MfdsItem item(String itemName, String entpName, String eeName, String spf, String pa) {
        return new MfdsItem(MfdsSource.REPORT, itemName, entpName, null, eeName, spf, pa,
                "제10조 제1항 제1호", "20240101", false);
    }

    @Test
    @DisplayName("등록명이 거의 일치하면 자동 확정하고 상태까지 옮긴다")
    void confirmsAndAdvancesStatus() {
        catalog.brand("구달", List.of(
                item("구달청귤비타씨잡티세럼", "(주)클리오",
                        "피부의 미백에 도움을 준다. 피부의 주름개선에 도움을 준다.", null, null)));

        FunctionalScreening screening = service().screen(product("청귤 비타씨 잡티 세럼", "구달", "treatments"));

        assertThat(screening.outcome()).isEqualTo(ScreeningOutcome.AUTO_CONFIRMED);
        assertThat(screening.claims()).containsExactlyInAnyOrder(
                ProductFunctionalCategory.WHITENING, ProductFunctionalCategory.WRINKLE_IMPROVEMENT);
        verify(productService).reviewFunction("p1", List.of("WHITENING", "WRINKLE_IMPROVEMENT"));
    }

    @Test
    @DisplayName("숫자가 다른 후보는 점수가 높아도 자동 확정하지 않는다")
    void doesNotConfirmWhenNumbersDiffer() {
        catalog.brand("닥터디퍼런트", List.of(
                item("닥터디퍼런트131모이스처라이저", "주식회사다른코스메틱스",
                        "피부의 주름개선에 도움을 준다.", null, null)));

        FunctionalScreening screening = service().screen(product("311 모이스처라이저", "닥터디퍼런트", "moisturizers"));

        assertThat(screening.outcome()).isEqualTo(ScreeningOutcome.NEEDS_REVIEW);
        assertThat(screening.hasCandidates()).isTrue(); // 후보는 보여 주되 확정하지 않는다
        verify(productService, never()).reviewFunction(anyString(), anyList());
    }

    @Test
    @DisplayName("선크림은 등록 건을 못 찾아도 '기능성 아님'으로 접지 않는다")
    void neverAutoConcludesNoneForSunscreen() {
        properties.setAutoConcludeNone(true);
        catalog.brand("디오디너리", List.of()); // 브랜드 등록 0건

        FunctionalScreening screening = service().screen(product("수분 선크림", "디오디너리", "sunscreens"));

        assertThat(screening.outcome()).isEqualTo(ScreeningOutcome.NOT_MATCHED);
        verify(productService, never()).reviewFunction(anyString(), anyList());
    }

    @Test
    @DisplayName("브랜드 등록이 0건이면 옵션을 켰을 때만 '기능성 아님'을 자동 확정한다")
    void autoConcludesNoneOnlyWhenEnabled() {
        catalog.brand("꼬달리", List.of());
        ProductResult target = product("뷰티 엘릭시르", "꼬달리", "toners");

        assertThat(service().screen(target).outcome()).isEqualTo(ScreeningOutcome.NOT_MATCHED);

        properties.setAutoConcludeNone(true);
        FunctionalScreening screening = service().screen(target);
        assertThat(screening.outcome()).isEqualTo(ScreeningOutcome.AUTO_NONE);
        assertThat(screening.claims()).isEmpty();
        verify(productService).reviewFunction("p1", List.of());
    }

    @Test
    @DisplayName("조회가 터지면 FAILED로 남기고 상태를 옮기지 않는다")
    void keepsFailuresOutOfTheDecision() {
        catalog.failWith(new IllegalStateException("안전나라 응답 오류 30: SERVICE KEY IS NOT REGISTERED"));

        FunctionalScreening screening = service().screen(product("청귤 세럼", "구달", "treatments"));

        assertThat(screening.outcome()).isEqualTo(ScreeningOutcome.FAILED);
        assertThat(screening.reason()).contains("SERVICE KEY");
        verify(productService, never()).reviewFunction(anyString(), anyList());
    }

    @Test
    @DisplayName("그림자 모드에서는 판정만 기록하고 상태는 그대로 둔다")
    void shadowModeRecordsWithoutAdvancing() {
        properties.setApplyDecisions(false);
        catalog.brand("구달", List.of(
                item("구달청귤비타씨잡티세럼", "(주)클리오", "피부의 미백에 도움을 준다.", null, null)));

        FunctionalScreening screening = service().screen(product("청귤 비타씨 잡티 세럼", "구달", "treatments"));

        assertThat(screening.outcome()).isEqualTo(ScreeningOutcome.AUTO_CONFIRMED);
        verify(repository).save(any());
        verify(productService, never()).reviewFunction(anyString(), anyList());
    }

    /** 안전나라 대역. item_name 부분 일치만 흉내 낸다 — 실제 API가 그것만 지원하기 때문이다. */
    private static class FakeCatalog implements MfdsCatalogPort {
        private final Map<String, List<MfdsItem>> byBrand = new HashMap<>();
        private RuntimeException failure;

        void brand(String brandKo, List<MfdsItem> items) {
            byBrand.put(brandKo, items);
        }

        void failWith(RuntimeException e) {
            this.failure = e;
        }

        @Override
        public List<MfdsItem> searchByItemName(String term) {
            if (failure != null) {
                throw failure;
            }
            List<MfdsItem> matched = new ArrayList<>();
            byBrand.values().forEach(items -> items.stream()
                    .filter(item -> item.itemName().contains(term) || term.contains(item.itemName()))
                    .forEach(matched::add));
            return matched;
        }

        @Override
        public List<MfdsItem> searchBrand(String brandKo) {
            if (failure != null) {
                throw failure;
            }
            return byBrand.getOrDefault(brandKo, List.of());
        }
    }
}
