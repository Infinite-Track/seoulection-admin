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
    @DisplayName("전성분만 저장해서는 다음 단계로 넘어가지 않는다 — 성분별 보완이 남아 있다")
    void savingIngredientsAloneDoesNotAdvance() {
        Product reviewed = crawledButTooFew()
                .reviewIngredients(List.of("Water", "Glycerin", "Niacinamide", "Panthenol", "Ceramide NP"), false);

        // 상태는 저장 전 그대로다 — 어드민 저장이 파이프라인 판정을 지우지도, 다음 단계로
        // 밀지도 않는다. 앞으로 미는 건 '성분 보완 완료' 뿐이다.
        assertThat(reviewed.status()).isEqualTo(ProductStatus.INSUFFICIENT_INGREDIENTS);
        assertThat(reviewed.ingredients()).hasSize(5);
    }

    @Test
    @DisplayName("성분별 보완을 마쳐야 성분 입력 완료가 된다")
    void completingReviewAdvances() {
        Product reviewed = crawledButTooFew()
                .reviewIngredients(List.of("Water", "Glycerin", "Niacinamide"), false);

        assertThat(reviewed.completeIngredientReview().status()).isEqualTo(ProductStatus.INGREDIENTS_ADDED);
    }

    @Test
    @DisplayName("함량을 하나도 안 채웠어도 완료할 수 있다 — 채울 값이 없는 제품이 있다")
    void completingWithoutConcentrationsIsAllowed() {
        Product reviewed = crawledButTooFew().reviewIngredients(List.of("Water"), false);

        assertThat(reviewed.completeIngredientReview().status()).isEqualTo(ProductStatus.INGREDIENTS_ADDED);
    }

    @Test
    @DisplayName("성분이 없으면 완료할 수 없다 — 아직 1단계도 끝나지 않았다")
    void cannotCompleteWithoutIngredients() {
        Product noIngredients = Product.builder()
                .name("시카 세럼").brand("서울렉션").category("treatments")
                .status(ProductStatus.NEED_MANUAL_REVIEW)
                .build();

        org.assertj.core.api.Assertions.assertThatThrownBy(noIngredients::completeIngredientReview)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("전성분");
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
