package com.seoulection.admin.product.presentation;

import com.seoulection.admin.product.application.service.ProductService;
import com.seoulection.admin.product.presentation.controller.ProductController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
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

    @Test
    @DisplayName("제품 등록 화면을 제공한다")
    void page() throws Exception {
        given(service.getProducts()).willReturn(List.of());

        mockMvc.perform(get("/admin/products"))
                .andExpect(status().isOk())
                .andExpect(view().name("products"))
                .andExpect(model().attributeExists("request", "products"));
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

        then(service).should().register("시카 세럼", "서울렉션", "face masks");
    }

    @Test
    @DisplayName("제품명, 브랜드, 카테고리는 모두 필수다")
    void rejectBlankFields() throws Exception {
        given(service.getProducts()).willReturn(List.of());

        mockMvc.perform(post("/admin/products")
                        .param("name", " ")
                        .param("brand", " ")
                        .param("category", " "))
                .andExpect(status().isOk())
                .andExpect(view().name("products"))
                .andExpect(model().attributeHasFieldErrors(
                        "request",
                        "name",
                        "brand",
                        "category"
                ));

        then(service).should().getProducts();
        then(service).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("정해진 목록에 없는 카테고리는 등록하지 않는다")
    void rejectUnsupportedCategory() throws Exception {
        given(service.getProducts()).willReturn(List.of());

        mockMvc.perform(post("/admin/products")
                        .param("name", "시카 세럼")
                        .param("brand", "서울렉션")
                        .param("category", "serum"))
                .andExpect(status().isOk())
                .andExpect(view().name("products"))
                .andExpect(model().attributeHasFieldErrors("request", "category"));

        then(service).should().getProducts();
        then(service).shouldHaveNoMoreInteractions();
    }
}
