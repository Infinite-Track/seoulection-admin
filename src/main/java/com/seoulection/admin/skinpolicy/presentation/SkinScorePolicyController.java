package com.seoulection.admin.skinpolicy.presentation;

import com.seoulection.admin.skinpolicy.application.SkinScorePolicyService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class SkinScorePolicyController {
    private final SkinScorePolicyService service;

    public SkinScorePolicyController(SkinScorePolicyService service) {
        this.service = service;
    }

    @GetMapping("/admin/skin-score-policy")
    public String page(Model model) {
        model.addAttribute("policies", service.findAll());
        return "skin-score-policy";
    }

    @PostMapping("/admin/skin-score-policy/{featureKey}")
    public String update(@PathVariable String featureKey,
                         @RequestParam double badBoundary,
                         @RequestParam double goodBoundary,
                         @RequestParam String policyVersion,
                         RedirectAttributes redirectAttributes) {
        try {
            service.update(featureKey, badBoundary, goodBoundary, policyVersion);
            redirectAttributes.addFlashAttribute("successMessage", featureKey + " 정책을 저장했습니다.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/skin-score-policy";
    }
}
