package com.seoulection.admin.product.presentation;

import com.seoulection.admin.product.application.dto.ProductPage;
import com.seoulection.admin.product.application.dto.ProductResult;
import com.seoulection.admin.product.application.service.ProductService;
import com.seoulection.admin.product.domain.enums.ProductStage;
import com.seoulection.admin.product.domain.enums.ProductStatus;
import com.seoulection.admin.product.presentation.controller.ProductController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ProductService service;

    @BeforeEach
    void stubList() {
        given(service.getProducts(any(), any(), anyInt(), anyInt()))
                .willReturn(new ProductPage(List.of(), 0, 25, 0, 0));
        given(service.countByStatus(any())).willReturn(Map.of());
    }

    @Test
    @DisplayName("제품 등록 화면을 제공한다")
    void page() throws Exception {
        mockMvc.perform(get("/admin/products"))
                .andExpect(status().isOk())
                .andExpect(view().name("products"))
                .andExpect(model().attributeExists("request", "page", "stages", "stageCounts"));
    }

    @Test
    @DisplayName("기본 탭은 성분 보완이다")
    void defaultsToIngredientsLane() throws Exception {
        mockMvc.perform(get("/admin/products"))
                .andExpect(model().attribute("selectedStageSlug", "ingredient-review"));

        then(service).should().getProducts(eq(ProductStage.INGREDIENT_REVIEW.statuses()), eq(null), eq(0), anyInt());
    }

    @Test
    @DisplayName("제품명, 브랜드, 카테고리로 제품을 등록한다")
    void register() throws Exception {
        mockMvc.perform(post("/admin/products")
                        .param("name", "시카 세럼")
                        .param("brand", "서울렉션")
                        .param("category", "face masks"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/products"))
                .andExpect(flash().attribute("successMessage", "제품을 등록했습니다."));

        then(service).should().register("시카 세럼", "서울렉션", "face masks", List.of());
    }

    @Test
    @DisplayName("제품명, 브랜드, 카테고리는 모두 필수다")
    void rejectBlankFields() throws Exception {
        mockMvc.perform(post("/admin/products")
                        .param("name", " ")
                        .param("brand", " ")
                        .param("category", " "))
                .andExpect(status().isOk())
                .andExpect(view().name("products"))
                .andExpect(model().attribute("registerFormOpen", true))
                .andExpect(model().attributeHasFieldErrors("request", "name", "brand", "category"));
    }

    @Test
    @DisplayName("정해진 목록에 없는 카테고리는 등록하지 않는다")
    void rejectUnsupportedCategory() throws Exception {
        mockMvc.perform(post("/admin/products")
                        .param("name", "시카 세럼")
                        .param("brand", "서울렉션")
                        .param("category", "serum"))
                .andExpect(status().isOk())
                .andExpect(view().name("products"))
                .andExpect(model().attributeHasFieldErrors("request", "category"));
    }

    @Test
    @DisplayName("검색어는 목록 조회와 집계에 모두 전달된다")
    void search() throws Exception {
        mockMvc.perform(get("/admin/products").param("q", "서울렉션"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("query", "서울렉션"));

        then(service).should().getProducts(eq(ProductStage.INGREDIENT_REVIEW.statuses()), eq("서울렉션"), eq(0), anyInt());
        then(service).should().countByStatus("서울렉션");
    }

    @Test
    @DisplayName("검색어가 공백뿐이면 검색이 아니다")
    void blankQueryIsNotSearch() throws Exception {
        mockMvc.perform(get("/admin/products").param("q", "   "))
                .andExpect(model().attribute("query", ""));

        then(service).should().getProducts(any(), eq(null), anyInt(), anyInt());
    }

    @Test
    @DisplayName("페이지 번호를 조회에 전달한다")
    void paging() throws Exception {
        mockMvc.perform(get("/admin/products").param("page", "2"))
                .andExpect(status().isOk());

        then(service).should().getProducts(any(), any(), eq(2), anyInt());
    }

    @Test
    @DisplayName("음수 페이지는 첫 페이지로 다룬다")
    void negativePageIsFirstPage() throws Exception {
        mockMvc.perform(get("/admin/products").param("page", "-3"))
                .andExpect(status().isOk());

        then(service).should().getProducts(any(), any(), eq(0), anyInt());
    }

    @Test
    @DisplayName("status로 탭 안의 한 상태만 좁혀 본다")
    void exactStatusFilter() throws Exception {
        mockMvc.perform(get("/admin/products")
                        .param("stage", "ingredient-failed")
                        .param("status", "INSUFFICIENT_INGREDIENTS"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("selectedStatus", "INSUFFICIENT_INGREDIENTS"));

        then(service).should().getProducts(eq(List.of(ProductStatus.INSUFFICIENT_INGREDIENTS)),
                any(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("탭에 속하지 않는 status는 무시하고 탭 전체를 보여 준다")
    void statusOutsideLaneIsIgnored() throws Exception {
        mockMvc.perform(get("/admin/products")
                        .param("stage", "ingredient-review")
                        .param("status", "COMPLETE"))
                .andExpect(status().isOk());

        then(service).should().getProducts(eq(ProductStage.INGREDIENT_REVIEW.statuses()), any(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("알 수 없는 status가 와도 목록은 깨지지 않는다")
    void unknownStatusIsIgnored() throws Exception {
        mockMvc.perform(get("/admin/products").param("status", "NOT_A_STATUS"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("selectedStatus", ""));

        then(service).should().getProducts(eq(ProductStage.INGREDIENT_REVIEW.statuses()), any(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("PENDING은 파이프라인 대기 탭에 있지만 성분 보완이 열려 있다")
    void pendingSitsInPipelineLaneButStaysEditable() throws Exception {
        org.assertj.core.api.Assertions.assertThat(ProductStatus.PENDING.stage()).isEqualTo(ProductStage.PIPELINE);
        // 파이프라인 탭에 있어도 어드민이 성분을 직접 넣을 수 있어야 한다.
        org.assertj.core.api.Assertions.assertThat(ProductStatus.PENDING.workflowStep()).isNull();
        org.assertj.core.api.Assertions.assertThat(ProductStatus.NEED_MANUAL_REVIEW.workflowStep()).isEqualTo("ingredients");
        org.assertj.core.api.Assertions.assertThat(ProductStage.INGREDIENT_REVIEW.statuses())
                .containsExactly(ProductStatus.NEED_MANUAL_REVIEW);
    }

    @Test
    @DisplayName("파이프라인 대기 탭 안에서 PENDING만 좁혀 볼 수 있다")
    void pendingIsFilterableInsidePipelineLane() throws Exception {
        mockMvc.perform(get("/admin/products").param("stage", "pipeline").param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("selectedStatus", "PENDING"));

        then(service).should().getProducts(eq(List.of(ProductStatus.PENDING)), any(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("성분 확보 실패 탭은 INSUFFICIENT_INGREDIENTS와 NOT_FOUND를 함께 담는다")
    void unavailableLane() throws Exception {
        org.assertj.core.api.Assertions.assertThat(ProductStage.INGREDIENT_FAILED.statuses())
                .containsExactlyInAnyOrder(ProductStatus.INSUFFICIENT_INGREDIENTS, ProductStatus.NOT_FOUND);

        mockMvc.perform(get("/admin/products").param("stage", "ingredient-failed"))
                .andExpect(status().isOk());

        then(service).should().getProducts(eq(ProductStage.INGREDIENT_FAILED.statuses()), any(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("전체 탭의 소계 칩은 모든 상태를 보여 준다")
    void allTabShowsEverySubstat() throws Exception {
        mockMvc.perform(get("/admin/products").param("stage", "all"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("substatStatuses", List.of(ProductStatus.values())));
    }

    @Test
    @DisplayName("제품 상세 화면을 제공한다")
    void detail() throws Exception {
        given(service.getProduct("abc")).willReturn(product());

        mockMvc.perform(get("/admin/products/abc"))
                .andExpect(status().isOk())
                .andExpect(view().name("product-detail"))
                .andExpect(model().attributeExists("product"));
    }

    @Test
    @DisplayName("제품을 삭제하면 목록으로 돌아간다")
    void delete() throws Exception {
        given(service.getProduct("abc")).willReturn(product());

        mockMvc.perform(post("/admin/products/abc/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/products"))
                .andExpect(flash().attributeExists("successMessage"));

        then(service).should().delete("abc");
    }

    @Test
    @DisplayName("기능성 확인을 골랐는데 유형이 없으면 저장하지 않는다")
    void rejectConfirmedWithoutFunction() throws Exception {
        mockMvc.perform(post("/admin/products/abc/workflow/functions")
                        .param("functionResult", "CONFIRMED"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("errorMessage"));

        then(service).should(never()).reviewFunction(any(), any());
    }

    @Test
    @DisplayName("조회 결과를 고르지 않으면 저장하지 않는다")
    void rejectMissingFunctionResult() throws Exception {
        mockMvc.perform(post("/admin/products/abc/workflow/functions"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("errorMessage"));

        then(service).should(never()).reviewFunction(any(), any());
    }

    @Test
    @DisplayName("미검수 제품은 조회 결과가 선택되지 않은 채로 열린다")
    void unreviewedProductHasNoPreselectedResult() throws Exception {
        given(service.getProduct("abc")).willReturn(product()); // INGREDIENTS_ADDED

        var request = (com.seoulection.admin.product.presentation.dto.ProductRegisterRequest)
                mockMvc.perform(get("/admin/products/abc/workflow"))
                        .andExpect(status().isOk())
                        .andReturn().getModelAndView().getModel().get("request");

        org.assertj.core.api.Assertions.assertThat(request.getFunctionResult()).isNull();
    }

    @Test
    @DisplayName("어드민이 할 일이 없는 제품은 작업 화면 대신 상세로 보낸다")
    void productWithoutAdminWorkRedirectsToDetail() throws Exception {
        given(service.getProduct("abc")).willReturn(reviewedProduct());

        mockMvc.perform(get("/admin/products/abc/workflow"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/products/abc"));
    }

    @Test
    @DisplayName("step을 지정하면 이미 지나간 단계도 다시 열 수 있다")
    void explicitStepReopensPastStage() throws Exception {
        given(service.getProduct("abc")).willReturn(reviewedProduct());

        var request = (com.seoulection.admin.product.presentation.dto.ProductRegisterRequest)
                mockMvc.perform(get("/admin/products/abc/workflow").param("step", "functional"))
                        .andExpect(status().isOk())
                        .andExpect(view().name("product-workflow"))
                        .andReturn().getModelAndView().getModel().get("request");

        // 이미 검수를 지난 제품이라 현재 결과가 찍혀 있어야 한다.
        org.assertj.core.api.Assertions.assertThat(request.getFunctionResult()).isEqualTo("NONE");
    }

    private ProductResult reviewedProduct() {
        return new ProductResult("abc", null, "시카 세럼", null, "서울렉션", "treatments", null, null, null, null,
                0L, BigDecimal.ZERO, null, "ADMIN", List.of("Water"), null, null,
                List.of(), ProductStatus.READY_FOR_INCIAPI);
    }

    @Test
    @DisplayName("기능성 아님을 고르면 유형을 남겨 두었어도 빈 목록으로 저장한다")
    void noneDiscardsFunction() throws Exception {
        mockMvc.perform(post("/admin/products/abc/workflow/functions")
                        .param("functionResult", "NONE")
                        .param("function", "WHITENING"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/products?stage=functional-review"));

        then(service).should().reviewFunction("abc", List.of());
    }

    @Test
    @DisplayName("성분을 찾지 못함을 고르고 성분을 적으면 저장하지 않는다")
    void rejectNotFoundWithIngredients() throws Exception {
        mockMvc.perform(post("/admin/products/abc/workflow/ingredients")
                        .param("ingredientResolution", "NOT_FOUND")
                        .param("ingredientsText", "Water"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("errorMessage"));

        then(service).should(never()).reviewIngredients(any(), any(), org.mockito.ArgumentMatchers.anyBoolean());
    }

    @Test
    @DisplayName("성분을 비운 채로는 저장하지 않는다 — 파이프라인 판정 상태를 어드민이 만들면 안 된다")
    void rejectEmptyIngredientsWhenFound() throws Exception {
        mockMvc.perform(post("/admin/products/abc/workflow/ingredients")
                        .param("ingredientResolution", "FOUND")
                        .param("ingredientsText", "   "))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("errorMessage"));

        then(service).should(never()).reviewIngredients(any(), any(), org.mockito.ArgumentMatchers.anyBoolean());
    }

    @Test
    @DisplayName("성분을 입력하면 저장한다")
    void saveIngredients() throws Exception {
        mockMvc.perform(post("/admin/products/abc/workflow/ingredients")
                        .param("ingredientResolution", "FOUND")
                        .param("ingredientsText", "Water, Glycerin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/products/abc/workflow?step=concentrations"));

        then(service).should().reviewIngredients("abc", List.of("Water", "Glycerin"), false);
    }

    private ProductResult product() {
        return new ProductResult("abc", null, "시카 세럼", null, "서울렉션", "treatments", null, null, null, null,
                0L, BigDecimal.ZERO, null, "ADMIN", List.of("Water"), null, null,
                List.of(), ProductStatus.INGREDIENTS_ADDED);
    }
}
