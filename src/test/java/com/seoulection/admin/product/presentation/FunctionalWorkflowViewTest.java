package com.seoulection.admin.product.presentation;

import com.seoulection.admin.product.application.dto.ProductResult;
import com.seoulection.admin.product.application.service.ProductService;
import com.seoulection.admin.product.domain.enums.ProductFunctionalCategory;
import com.seoulection.admin.product.domain.enums.ProductStatus;
import com.seoulection.admin.product.functional.application.FunctionalScreeningService;
import com.seoulection.admin.product.functional.domain.ClaimReading;
import com.seoulection.admin.product.functional.domain.FunctionalScreening;
import com.seoulection.admin.product.functional.domain.MfdsCandidate;
import com.seoulection.admin.product.functional.domain.MfdsItem;
import com.seoulection.admin.product.functional.domain.MfdsSource;
import com.seoulection.admin.product.functional.domain.ScreeningOutcome;
import com.seoulection.admin.product.presentation.controller.ProductController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 기능성 탭이 자동 조회 결과를 실제로 그려 내는지 본다.
 *
 * <p>이 테스트가 있는 이유: Thymeleaf 표현식 오류는 컴파일에 걸리지 않고 화면을 열어야만
 * 드러난다. 특히 enum 메서드는 {@code status.tone()}처럼 괄호를 붙여야 하고, 레코드 컴포넌트는
 * 안 붙여야 한다 — 한 글자 차이로 500이 난다.
 */
@WebMvcTest(ProductController.class)
class FunctionalWorkflowViewTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ProductService service;

    @MockitoBean
    FunctionalScreeningService screeningService;

    @Test
    @DisplayName("확정하지 못한 판정은 후보 표와 보류 사유까지 그린다")
    void rendersCandidatesAndBlockReason() throws Exception {
        given(service.getProduct("p1")).willReturn(product());
        given(screeningService.findOrScreen("p1")).willReturn(Optional.of(screening()));

        mockMvc.perform(get("/admin/products/p1/workflow").param("step", "functional"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("닥터디퍼런트131모이스처라이저")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("확인 필요")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("저장하고 자동 조회")));
    }

    @Test
    @DisplayName("판정이 없어도 화면은 뜬다 — 한글 이름 입력 칸만 보인다")
    void rendersWithoutScreening() throws Exception {
        given(service.getProduct("p1")).willReturn(product());
        given(screeningService.findOrScreen("p1")).willReturn(Optional.empty());

        mockMvc.perform(get("/admin/products/p1/workflow").param("step", "functional"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("저장하고 자동 조회")));
    }

    private ProductResult product() {
        return new ProductResult("p1", null, "Dr.different 311 Moisturizer", "311 모이스처라이저",
                "닥터디퍼런트", "moisturizers", null, BigDecimal.ZERO, null, null, 0,
                BigDecimal.ZERO, null, "ADMIN", List.of("정제수", "글리세린"), Map.of(), null,
                List.of(), ProductStatus.INGREDIENTS_ADDED);
    }

    private FunctionalScreening screening() {
        MfdsItem item = new MfdsItem(MfdsSource.REPORT, "닥터디퍼런트131모이스처라이저",
                "주식회사다른코스메틱스", "2", "피부의 주름개선에 도움을 준다.", null, null,
                "제10조 제1항 제1호", "20230405", false);
        MfdsCandidate candidate = new MfdsCandidate(item, 0.94, 0.94,
                new ClaimReading(List.of(ProductFunctionalCategory.WRINKLE_IMPROVEMENT), false, true),
                false, true);
        return new FunctionalScreening("p1", ScreeningOutcome.NEEDS_REVIEW, List.of(),
                List.of(candidate), -1, "LOW", "제품명 숫자가 달라 확정을 보류했습니다", 37,
                FunctionalScreening.DECIDED_BY_AUTO, FunctionalScreening.ENGINE_VERSION, Instant.now());
    }
}
