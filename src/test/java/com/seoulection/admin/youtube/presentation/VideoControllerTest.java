package com.seoulection.admin.youtube.presentation;

import com.seoulection.admin.youtube.application.service.VideoService;
import com.seoulection.admin.youtube.presentation.controller.VideoController;
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

@WebMvcTest(VideoController.class)
class VideoControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    VideoService service;

    @Test
    @DisplayName("영상 링크 등록 화면을 별도로 제공한다")
    void page() throws Exception {
        given(service.getVideos()).willReturn(List.of());

        mockMvc.perform(get("/admin/videos"))
                .andExpect(status().isOk())
                .andExpect(view().name("videos"))
                .andExpect(model().attributeExists("request", "videos"));
    }

    @Test
    @DisplayName("영상 링크를 등록한다")
    void register() throws Exception {
        String url = "https://youtu.be/dQw4w9WgXcQ";

        mockMvc.perform(post("/admin/videos").param("url", url))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/videos"))
                .andExpect(flash().attribute("successMessage", "YouTube 영상을 등록했습니다."));

        then(service).should().register(url);
    }
}
