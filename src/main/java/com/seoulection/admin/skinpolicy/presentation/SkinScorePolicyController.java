package com.seoulection.admin.skinpolicy.presentation;

import com.seoulection.admin.skinpolicy.application.SkinScorePolicyService;
import com.seoulection.admin.skinpolicy.application.SkinTypeRuleService;
import com.seoulection.admin.skinpolicy.domain.SkinScorePolicy;
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
    private final SkinTypeRuleService skinTypeRuleService;

    public SkinScorePolicyController(SkinScorePolicyService service, SkinTypeRuleService skinTypeRuleService) {
        this.service = service;
        this.skinTypeRuleService = skinTypeRuleService;
    }

    @GetMapping("/admin/skin-score-policy")
    public String page(Model model) {
        model.addAttribute("policies", service.findAll());
        model.addAttribute("skinTypeRule", skinTypeRuleService.get());
        model.addAttribute("oilQuestions", skinTypeRuleService.oilQuestionCandidates());
        return "skin-score-policy";
    }

    @PostMapping("/admin/skin-score-policy/{featureKey}")
    public String update(@PathVariable String featureKey,
                         @RequestParam double badBoundary,
                         @RequestParam double goodBoundary,
                         @RequestParam String policyVersion,
                         @RequestParam(required = false) Double minAnsweredWeight,
                         RedirectAttributes redirectAttributes) {
        try {
            double minAnswered = minAnsweredWeight == null
                    ? SkinScorePolicy.DEFAULT_MIN_ANSWERED_WEIGHT : minAnsweredWeight;
            service.update(featureKey, badBoundary, goodBoundary, policyVersion, minAnswered);
            redirectAttributes.addFlashAttribute("successMessage", featureKey + " 정책을 저장했습니다.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/skin-score-policy";
    }

    /**
     * 설문 피부타입 계산 규칙(한 행). 경로가 {@code /{featureKey}} 와 겹치지 않도록 앞에 둔다 — 스프링은 리터럴
     * 경로를 변수 경로보다 먼저 맞춘다.
     */
    @PostMapping("/admin/skin-score-policy/skin-type-rule")
    public String updateSkinTypeRule(@RequestParam String tZoneQuestionKey,
                                     @RequestParam String cheekQuestionKey,
                                     @RequestParam int oilyMax,
                                     @RequestParam int clearMin,
                                     @RequestParam int dryWaterMax,
                                     @RequestParam(defaultValue = "false") boolean preferSelfDeclared,
                                     RedirectAttributes redirectAttributes) {
        try {
            skinTypeRuleService.update(tZoneQuestionKey, cheekQuestionKey, oilyMax, clearMin, dryWaterMax,
                    preferSelfDeclared);
            redirectAttributes.addFlashAttribute("successMessage",
                    "피부타입 계산 규칙을 저장했습니다. 다음 추천 계산부터 반영됩니다.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/skin-score-policy";
    }
}
