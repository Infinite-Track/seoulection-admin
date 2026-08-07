package com.seoulection.admin.survey.presentation.controller;

import com.seoulection.admin.survey.application.service.SurveyAdminService;
import com.seoulection.admin.survey.presentation.dto.SurveyOptionCreateRequest;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 설문 문항·선택지 관리 화면.
 *
 * <p>수정/삭제까지 전부 {@code POST}인 이유: HTML 폼은 GET/POST만 보낼 수 있다(PUT/DELETE 불가).
 * 히든 메서드 필터를 켜는 대신 경로로 의도를 드러낸다 — 이 저장소의 다른 관리 화면과도 같은 방식이다.
 */
@Controller
public class SurveyAdminController {

    private final SurveyAdminService service;

    public SurveyAdminController(SurveyAdminService service) {
        this.service = service;
    }

    @GetMapping("/admin/survey")
    public String page(Model model) {
        if (!model.containsAttribute("request")) {
            model.addAttribute("request", new SurveyOptionCreateRequest());
        }
        model.addAttribute("questions", service.getQuestions());
        return "survey";
    }

    @PostMapping("/admin/survey/options")
    public String createOption(@Valid @ModelAttribute("request") SurveyOptionCreateRequest request,
                               BindingResult bindingResult,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("questions", service.getQuestions());
            return "survey";
        }

        try {
            service.createOption(request.getQuestionKey(), request.getCode(), request.getLabel(),
                    request.getValue(), request.getSortOrder(), request.isExclusive());
        } catch (IllegalArgumentException e) {
            // 코드 중복·형식 위반은 사용자가 고칠 수 있는 입력 오류다 → 폼으로 되돌려 사유를 보여준다.
            bindingResult.rejectValue("code", "invalid", e.getMessage());
            model.addAttribute("questions", service.getQuestions());
            return "survey";
        }
        redirectAttributes.addFlashAttribute("successMessage", "선택지를 추가했습니다.");
        return "redirect:/admin/survey";
    }

    @PostMapping("/admin/survey/options/{optionId}")
    public String updateOption(@PathVariable Long optionId,
                               @RequestParam String label,
                               @RequestParam(required = false) Integer value,
                               @RequestParam int sortOrder,
                               @RequestParam(defaultValue = "false") boolean exclusive,
                               RedirectAttributes redirectAttributes) {
        service.updateOption(optionId, label, value, sortOrder, exclusive);
        redirectAttributes.addFlashAttribute("successMessage", "선택지를 수정했습니다.");
        return "redirect:/admin/survey";
    }

    /** 숨김/되살리기. 행을 지우지 않는 이유는 서비스 주석 참조. */
    @PostMapping("/admin/survey/options/{optionId}/active")
    public String changeOptionActive(@PathVariable Long optionId,
                                     @RequestParam boolean active,
                                     RedirectAttributes redirectAttributes) {
        service.changeOptionActive(optionId, active);
        redirectAttributes.addFlashAttribute("successMessage",
                active ? "선택지를 다시 노출합니다." : "선택지를 숨겼습니다. 기존 응답은 그대로 남습니다.");
        return "redirect:/admin/survey";
    }

    @PostMapping("/admin/survey/questions/{questionKey}")
    public String updateQuestionTitle(@PathVariable String questionKey,
                                      @RequestParam String title,
                                      RedirectAttributes redirectAttributes) {
        service.updateQuestionTitle(questionKey, title);
        redirectAttributes.addFlashAttribute("successMessage", "질문 문구를 수정했습니다.");
        return "redirect:/admin/survey";
    }
}
