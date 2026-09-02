package com.seoulection.admin.product.domain;

import com.seoulection.admin.product.domain.entity.Product;
import com.seoulection.admin.product.domain.enums.ProductStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * INSUFFICIENT_INGREDIENTS(크롤링 성분 5개 미만)와 NOT_FOUND(성분 정보 없음)는 파이프라인의
 * 판정이다. 어드민 저장이 이 둘을 만들어 내지 않는지 고정해 둔다 — NOT_FOUND는 어드민이
 * "찾지 못함"을 명시적으로 고른 경우에만 나온다.
 */
class ProductStatusTransitionTest {

    private Product crawledButTooFew() {
        return Product.builder()
                .name("시카 세럼").brand("서울렉션").category("treatments")
                .ingredients(List.of("Water", "Glycerin"))
                .status(ProductStatus.INSUFFICIENT_INGREDIENTS)
                .build();
    }

    @Test
    @DisplayName("성분을 채워 저장하면 기능성 확인 단계로 넘어간다")
    void fillingIngredientsMovesToFunctionalQueue() {
        Product reviewed = crawledButTooFew()
                .reviewIngredients(List.of("Water", "Glycerin", "Niacinamide", "Panthenol", "Ceramide NP"), false);

        assertThat(reviewed.status()).isEqualTo(ProductStatus.INGREDIENTS_ADDED);
        assertThat(reviewed.ingredientSource()).isEqualTo(Product.ADMIN_SOURCE);
    }

    @Test
    @DisplayName("빈 성분으로 저장해도 상태는 그대로다 — 어드민이 파이프라인 판정을 만들지 않는다")
    void emptySaveKeepsStatus() {
        assertThat(crawledButTooFew().reviewIngredients(List.of(), false).status())
                .isEqualTo(ProductStatus.INSUFFICIENT_INGREDIENTS);

        Product pending = Product.pending("레티놀 앰플", "토리든", "treatments");
        assertThat(pending.reviewIngredients(List.of(), false).status())
                .isEqualTo(ProductStatus.PENDING);
    }

    @Test
    @DisplayName("찾지 못함을 고른 경우에만 NOT_FOUND가 된다")
    void notFoundOnlyWhenExplicit() {
        assertThat(crawledButTooFew().reviewIngredients(List.of(), true).status())
                .isEqualTo(ProductStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("기능성 검수를 지난 제품은 성분을 고쳐도 앞 단계로 되돌아가지 않는다")
    void reviewedProductKeepsPipelineStage() {
        Product ready = Product.builder()
                .name("선크림").brand("라운드랩").category("sunscreens")
                .ingredients(List.of("Water"))
                .status(ProductStatus.READY_FOR_ANALYSIS)
                .build();

        assertThat(ready.reviewIngredients(List.of("Water", "Zinc Oxide"), false).status())
                .isEqualTo(ProductStatus.READY_FOR_ANALYSIS);
    }
}
