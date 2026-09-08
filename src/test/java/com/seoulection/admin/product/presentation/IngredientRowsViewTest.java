package com.seoulection.admin.product.presentation;

import com.seoulection.admin.product.application.dto.ProductIngredientProperty;
import com.seoulection.admin.product.application.dto.ProductIngredientResult;
import com.seoulection.admin.product.application.dto.ProductResult;
import com.seoulection.admin.product.application.dto.PropertyDefinitionResult;
import com.seoulection.admin.product.application.service.ProductService;
import com.seoulection.admin.product.domain.enums.ProductStatus;
import com.seoulection.admin.product.functional.application.FunctionalScreeningService;
import com.seoulection.admin.product.presentation.controller.ProductController;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 성분 보완 화면(ingredient-rows 조각).
 *
 * <p>첫 번째 테스트가 특히 중요하다 — 특성이 <b>하나라도 붙은</b> 행에서만 터지던 버그를 막는다.
 * 특성이 비어 있으면 SpEL 선택식이 원소를 한 번도 평가하지 않아 오류가 드러나지 않는다. 그래서
 * 특성이 없는 개발 DB에서는 멀쩡하다가, 실제로 값을 채우는 순간 화면이 흰 화면이 됐다
 * (Thymeleaf가 출력을 이미 내보낸 뒤라 에러 페이지조차 못 그린다).
 */
@WebMvcTest(ProductController.class)
class IngredientRowsViewTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ProductService service;

    @MockitoBean
    FunctionalScreeningService screeningService;

    private void stubProduct() {
        given(service.getProduct("p1")).willReturn(new ProductResult(
                "p1", null, "Goodal Serum", "구달 청귤 비타C 잡티 세럼", "구달", "treatments", null,
                null, null, null, 0, BigDecimal.ZERO, null, "PIPELINE",
                List.of("나이아신아마이드", "청귤껍질추출물"), Map.of(), null,
                List.of(), ProductStatus.NEED_MANUAL_REVIEW));
        given(service.propertyDefinitions()).willReturn(List.of(
                new PropertyDefinitionResult("PURITY", "순도", "NUMBER", "%", "원료 순도"),
                new PropertyDefinitionResult("FORM", "형태", "TEXT", null, "분말/액상 등")));
    }

    private ProductIngredientResult matched(long id, String rawName, List<ProductIngredientProperty> properties) {
        return new ProductIngredientResult(id, "00000000-0000-0000-0000-000000000107", rawName, (int) id,
                new BigDecimal("2.0"), new BigDecimal("2.0"), "%", null,
                "Niacinamide", "나이아신아마이드", properties);
    }

    private ProductIngredientResult unmatched(long id, String rawName) {
        return new ProductIngredientResult(id, null, rawName, (int) id, null, null, null, null,
                null, null, List.of());
    }

    @Test
    @DisplayName("특성이 채워진 행도 렌더링된다 — 값이 입력 칸에 다시 찍힌다")
    void rendersRowWithExistingProperties() throws Exception {
        stubProduct();
        given(service.getProductIngredients("p1")).willReturn(List.of(
                matched(3, "나이아신아마이드", List.of(new ProductIngredientProperty(
                        "PURITY", "순도", "NUMBER", null,
                        new BigDecimal("98.5"), new BigDecimal("99.9"), "%", null)))));

        mockMvc.perform(get("/admin/products/p1/workflow").param("step", "ingredients"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("98.5")))
                .andExpect(content().string(Matchers.containsString("나이아신아마이드")));
    }

    @Test
    @DisplayName("사전에 없는 성분은 입력 폼 대신 '보완하지 않는 성분'으로만 보인다")
    void unmatchedIngredientsGetNoForm() throws Exception {
        stubProduct();
        given(service.getProductIngredients("p1")).willReturn(List.of(
                matched(3, "나이아신아마이드", List.of()),
                unmatched(7, "청귤껍질추출물")));

        String html = mockMvc.perform(get("/admin/products/p1/workflow").param("step", "ingredients"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // 폼은 사전 연결된 행 하나만. ("/workflow/ingredients/complete" 는 보완 완료 버튼이라 제외)
        long forms = html.lines()
                .filter(line -> line.contains("/ingredients/") && !line.contains("/workflow/ingredients/"))
                .count();
        org.assertj.core.api.Assertions.assertThat(forms).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(html).contains("/ingredients/3");
        org.assertj.core.api.Assertions.assertThat(html).doesNotContain("/ingredients/7");
        // 그래도 사라지지는 않는다 — 왜 입력 칸이 없는지 화면에서 알 수 있어야 한다.
        org.assertj.core.api.Assertions.assertThat(html).contains("사전에 없어 보완하지 않는 성분");
        org.assertj.core.api.Assertions.assertThat(html).contains("청귤껍질추출물");
    }

    @Test
    @DisplayName("사전 연결된 성분이 하나도 없으면 그 이유를 알려 준다")
    void explainsWhenNothingIsLinked() throws Exception {
        stubProduct();
        given(service.getProductIngredients("p1")).willReturn(List.of(unmatched(7, "청귤껍질추출물")));

        mockMvc.perform(get("/admin/products/p1/workflow").param("step", "ingredients"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("보완할 항목이 없습니다")));
    }
}
