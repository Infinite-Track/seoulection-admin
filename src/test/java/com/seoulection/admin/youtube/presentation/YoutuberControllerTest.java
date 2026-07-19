package com.seoulection.admin.youtube.presentation;

import com.seoulection.admin.youtube.application.service.YoutuberService;
import com.seoulection.admin.youtube.presentation.controller.YoutuberController;
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

@WebMvcTest(YoutuberController.class)
class YoutuberControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    YoutuberService service;

    @Test
    @DisplayName("유튜버 채널 등록 화면을 별도로 제공한다")
    void page() throws Exception {
        given(service.getYoutubers()).willReturn(List.of());

        mockMvc.perform(get("/admin/youtubers"))
                .andExpect(status().isOk())
                .andExpect(view().name("youtubers"))
                .andExpect(model().attributeExists("request", "youtubers"));
    }

    @Test
    @DisplayName("채널 링크를 등록한다")
    void register() throws Exception {
        String url = "https://www.youtube.com/@beauty";

        mockMvc.perform(post("/admin/youtubers")
                        .param("url", url))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/youtubers"))
                .andExpect(flash().attribute("successMessage", "YouTube 채널을 등록했습니다."));

        then(service).should().register(url);
    }
}
