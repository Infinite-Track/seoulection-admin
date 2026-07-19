package com.seoulection.admin.youtube.presentation.controller;

import com.seoulection.admin.youtube.application.service.YoutuberService;
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
public class YoutuberController {

    private final YoutuberService service;

    public YoutuberController(YoutuberService service) {
        this.service = service;
    }

    @GetMapping("/admin/youtubers")
    public String page(Model model) {
        if (!model.containsAttribute("request")) {
            model.addAttribute("request", new YoutubeUrlRegisterRequest());
        }
        model.addAttribute("youtubers", service.getYoutubers());
        return "youtubers";
    }

    @PostMapping("/admin/youtubers")
    public String register(
            @Valid @ModelAttribute("request") YoutubeUrlRegisterRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("youtubers", service.getYoutubers());
            return "youtubers";
        }

        try {
            service.register(request.getUrl());
            redirectAttributes.addFlashAttribute("successMessage", "YouTube 채널을 등록했습니다.");
            return "redirect:/admin/youtubers";
        } catch (YoutubeAdminException e) {
            bindingResult.rejectValue("url", e.reason().name(), e.reason().userMessage());
            model.addAttribute("youtubers", service.getYoutubers());
            return "youtubers";
        }
    }
}
