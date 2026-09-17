package com.seoulection.admin.product.presentation;

import com.seoulection.admin.product.application.dto.*;
import com.seoulection.admin.product.application.service.ProductService;
import com.seoulection.admin.product.domain.enums.ProductStatus;
import com.seoulection.admin.product.functional.application.*;
import com.seoulection.admin.product.presentation.controller.ProductController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

@WebMvcTest(ProductController.class)
class FunctionalScreeningListViewTest {
    @Autowired MockMvc mvc;
    @MockitoBean ProductService products;
    @MockitoBean FunctionalScreeningService screening;
    @MockitoBean FunctionalScreeningList list;
    @MockitoBean FunctionalScreeningQueueSettings settings;

    @Test void rendersSettingsWithinFunctionalTabWithoutProductList() throws Exception {
        when(products.getProducts(any(),any(),anyInt(),anyInt())).thenReturn(new ProductPage(List.of(),0,25,0,0));
        when(products.countByStatus(any())).thenReturn(Map.of());
        when(settings.get()).thenReturn(new FunctionalScreeningQueueSettings.Settings(true, 1, 5));
        when(settings.activeCount()).thenReturn(2L);
        mvc.perform(get("/admin/products").param("stage","functional-review").param("tab","settings"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("자동 등록 활성화")))
            .andExpect(content().string(containsString("2 / 5개")))
            .andExpect(content().string(containsString("role=\"switch\"")))
            .andExpect(content().string(org.hamcrest.Matchers.not(containsString("등록된 제품"))));
        verifyNoInteractions(list);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"oldest", "newest"})
    void rendersStateCountsAndPreservesFilterInPagination(String order) throws Exception {
        var product=new ProductResult("p1", "ASIN", "Serum", "세럼", "Brand", "treatments", null,
            BigDecimal.ZERO,null,null,0,BigDecimal.ZERO,null,null,List.of(),Map.of(),null,List.of(),ProductStatus.INGREDIENTS_ADDED);
        when(products.getProducts(any(),any(),anyInt(),anyInt())).thenReturn(new ProductPage(List.of(),0,25,0,0));
        when(products.countByStatus(any())).thenReturn(Map.of());
        Map<FunctionalScreeningList.State,Long> counts=new EnumMap<>(FunctionalScreeningList.State.class);
        for(var state:FunctionalScreeningList.State.values()) counts.put(state,0L);
        counts.put(FunctionalScreeningList.State.COMPLETED,26L);
        when(list.find(null,"COMPLETED",0,25,order)).thenReturn(new FunctionalScreeningList.Result(
            new ProductPage(List.of(product),0,25,26,2),Map.of("p1",FunctionalScreeningList.State.COMPLETED),counts));
        mvc.perform(get("/admin/products").param("stage","functional-review").param("screeningStatus","COMPLETED").param("order",order))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("조회 완료 26")))
            .andExpect(content().string(containsString("기능성 확정이 아닙니다")))
            .andExpect(content().string(containsString("screeningStatus=COMPLETED")))
            .andExpect(content().string(containsString("name=\"order\" value=\"" + order + "\"")))
            .andExpect(content().string(containsString("order=" + order + "&amp;page=1")));
        verify(list).find(null,"COMPLETED",0,25,order);
    }

    @Test void classifiesMissingNameAndQueueStates() {
        org.assertj.core.api.Assertions.assertThat(FunctionalScreeningList.classify(" ","COMPLETED")).isEqualTo(FunctionalScreeningList.State.NAME_REQUIRED);
        org.assertj.core.api.Assertions.assertThat(FunctionalScreeningList.classify("세럼",null)).isEqualTo(FunctionalScreeningList.State.UNREGISTERED);
        org.assertj.core.api.Assertions.assertThat(FunctionalScreeningList.classify("세럼","QUEUED")).isEqualTo(FunctionalScreeningList.State.QUEUED);
        org.assertj.core.api.Assertions.assertThat(FunctionalScreeningList.classify("세럼","FAILED")).isEqualTo(FunctionalScreeningList.State.FAILED);
    }
}
