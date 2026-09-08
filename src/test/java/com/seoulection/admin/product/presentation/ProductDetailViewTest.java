package com.seoulection.admin.product.presentation;

import com.seoulection.admin.product.application.dto.ProductResult;
import com.seoulection.admin.product.application.service.ProductService;
import com.seoulection.admin.product.domain.enums.ProductStatus;
import com.seoulection.admin.product.functional.application.FunctionalScreeningService;
import com.seoulection.admin.product.presentation.controller.ProductController;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 상세 화면이 "수집 전" 상태의 빈 제품에서도 렌더링되는지 본다. */
@WebMvcTest(ProductController.class)
class ProductDetailViewTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ProductService service;

    @MockitoBean
    FunctionalScreeningService screeningService;

    @Test
    @DisplayName("갓 등록해 아무것도 채워지지 않은 PENDING 제품의 상세도 열린다")
    void rendersBarePendingProduct() throws Exception {
        given(service.getProduct("p1")).willReturn(new ProductResult(
                "p1", null, "ㅁㄴㅇㄹ", null, "ㅁㄴㅇㄹ", "toners", null,
                null, null, null, 0, BigDecimal.ZERO, null, null,
                null, null, null, List.of(), ProductStatus.PENDING));

        mockMvc.perform(get("/admin/products/p1")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("파이프라인이 채운 값이 다 있는 제품의 상세도 열린다")
    void rendersFullyPopulatedProduct() throws Exception {
        given(service.getProduct("p2")).willReturn(new ProductResult(
                "p2", "B01ABC", "Goodal Serum", "구달 청귤 세럼", "구달", "treatments", "설명",
                new BigDecimal("19900"), "https://img", "https://shop", 12,
                new BigDecimal("0.3"), new BigDecimal("1.2"), "ADMIN",
                List.of("정제수"), Map.of("k", "v"), java.time.Instant.now(),
                List.of(), ProductStatus.COMPLETE));

        mockMvc.perform(get("/admin/products/p2")).andExpect(status().isOk());
    }
}
