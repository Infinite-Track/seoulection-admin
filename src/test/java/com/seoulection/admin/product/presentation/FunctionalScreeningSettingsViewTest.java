package com.seoulection.admin.product.presentation;

import com.seoulection.admin.product.functional.application.FunctionalScreeningQueueSettings;
import com.seoulection.admin.product.functional.application.FunctionalScreeningService;
import com.seoulection.admin.product.functional.presentation.FunctionalScreeningSettingsController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

@WebMvcTest(FunctionalScreeningSettingsController.class)
class FunctionalScreeningSettingsViewTest {
    @Autowired MockMvc mvc;
    @MockitoBean FunctionalScreeningQueueSettings settings;
    @MockitoBean FunctionalScreeningService screening;

    @Test void legacySettingsAddressRedirectsToFunctionalTab() throws Exception {
        mvc.perform(get("/admin/functional-screening-settings"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/products?stage=functional-review&tab=settings"));
    }

    @Test void uncheckedCheckboxDisablesOnlyAutomaticRegistration() throws Exception {
        mvc.perform(post("/admin/functional-screening-settings").param("scanIntervalMinutes", "10").param("capacity", "50"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/products?stage=functional-review&tab=settings"))
                .andExpect(flash().attributeExists("successMessage"));
        verify(settings).update(false, 10, 50);
    }

    @Test void invalidRangeShowsGuidance() throws Exception {
        doThrow(new IllegalArgumentException("대기열 한도는 1~1000개로 입력해 주세요."))
                .when(settings).update(true, 5, 0);
        mvc.perform(post("/admin/functional-screening-settings").param("autoEnqueue", "true")
                .param("scanIntervalMinutes", "5").param("capacity", "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("errorMessage"));
    }
}
