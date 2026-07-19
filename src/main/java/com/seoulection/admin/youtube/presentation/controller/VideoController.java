package com.seoulection.admin.youtube.presentation.controller;

import com.seoulection.admin.youtube.application.service.VideoService;
import com.seoulection.admin.youtube.domain.exception.YoutubeAdminException;
import com.seoulection.admin.youtube.presentation.dto.YoutubeUrlRegisterRequest;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class VideoController {

    private final VideoService service;

    public VideoController(VideoService service) {
        this.service = service;
    }

    @GetMapping({"/admin/videos", "/admin/youtube-videos"})
    public String page(Model model) {
        if (!model.containsAttribute("request")) {
            model.addAttribute("request", new YoutubeUrlRegisterRequest());
        }
        model.addAttribute("videos", service.getVideos());
        return "videos";
    }

    @PostMapping({"/admin/videos", "/admin/youtube-videos"})
    public String register(
            @Valid @ModelAttribute("request") YoutubeUrlRegisterRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("videos", service.getVideos());
            return "videos";
        }

        try {
            service.register(request.getUrl());
            redirectAttributes.addFlashAttribute("successMessage", "YouTube 영상을 등록했습니다.");
            return "redirect:/admin/videos";
        } catch (YoutubeAdminException e) {
            bindingResult.rejectValue("url", e.reason().name(), e.reason().userMessage());
            model.addAttribute("videos", service.getVideos());
            return "videos";
        }
    }
}
