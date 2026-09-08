package com.seoulection.admin.survey.presentation.controller;

import com.seoulection.admin.survey.application.service.SurveyAdminService;
import com.seoulection.admin.survey.presentation.dto.SurveyOptionCreateRequest;
import com.seoulection.admin.survey.presentation.dto.SurveyQuestionCreateRequest;
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

    private final com.seoulection.admin.survey.application.service.SurveyEvidenceService evidenceService;

    private final SurveyAdminService service;

    public SurveyAdminController(SurveyAdminService service, com.seoulection.admin.survey.application.service.SurveyEvidenceService evidenceService) {
        this.evidenceService = evidenceService;
        this.service = service;
    }

    @GetMapping("/admin/survey")
    public String page(Model model) {
        if (!model.containsAttribute("request")) {
            model.addAttribute("request", new SurveyOptionCreateRequest());
        }
        if (!model.containsAttribute("questionRequest")) {
            model.addAttribute("questionRequest", new SurveyQuestionCreateRequest());
        }
        populateSurveyModel(model);
        return "survey";
    }

    /**
     * 문항 추가. api-server가 문항 집합을 코드에 고정하지 않으므로 여기서 자유롭게 늘릴 수 있다 —
     * 단 {@code questionKey}는 자연 PK라 중복이면 거부한다(서비스 주석 참조).
     */
    @PostMapping("/admin/survey/questions")
    public String createQuestion(@Valid @ModelAttribute("questionRequest") SurveyQuestionCreateRequest request,
                                 BindingResult bindingResult,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("request", new SurveyOptionCreateRequest());
            populateSurveyModel(model);
            model.addAttribute("questionFormOpen", true);
            return "survey";
        }

        try {
            service.createQuestion(request.getQuestionKey(), request.getTitle(), request.getSortOrder());
        } catch (IllegalArgumentException e) {
            bindingResult.rejectValue("questionKey", "invalid", e.getMessage());
            model.addAttribute("request", new SurveyOptionCreateRequest());
            populateSurveyModel(model);
            model.addAttribute("questionFormOpen", true);
            return "survey";
        }
        redirectAttributes.addFlashAttribute("successMessage", "문항을 추가했습니다.");
        return "redirect:/admin/survey";
    }

    @PostMapping("/admin/survey/options")
    public String createOption(@Valid @ModelAttribute("request") SurveyOptionCreateRequest request,
                               BindingResult bindingResult,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("questionRequest", new SurveyQuestionCreateRequest());
            populateSurveyModel(model);
            model.addAttribute("optionFormOpen", true);
            return "survey";
        }

        try {
            service.createOption(request.getQuestionKey(), request.getCode(), request.getLabel(),
                    request.getValue(), request.getSortOrder(), request.isExclusive());
        } catch (IllegalArgumentException e) {
            // 코드 중복·형식 위반은 사용자가 고칠 수 있는 입력 오류다 → 폼으로 되돌려 사유를 보여준다.
            bindingResult.rejectValue("code", "invalid", e.getMessage());
            model.addAttribute("questionRequest", new SurveyQuestionCreateRequest());
            populateSurveyModel(model);
            model.addAttribute("optionFormOpen", true);
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

    /** 숨김/되살리기. 이것이 문항 삭제다 — 행을 지우지 않는 이유는 서비스 주석 참조. */
    @PostMapping("/admin/survey/questions/{questionKey}/active")
    public String changeQuestionActive(@PathVariable String questionKey,
                                       @RequestParam boolean active,
                                       RedirectAttributes redirectAttributes) {
        service.changeQuestionActive(questionKey, active);
        redirectAttributes.addFlashAttribute("successMessage",
                active ? "문항을 다시 노출합니다." : "문항을 숨겼습니다. 기존 응답은 그대로 남습니다.");
        return "redirect:/admin/survey";
    }

    // ── 문항 근거 ─────────────────────────────────────────────────────────
    // 설문이 "근거 기반"이어야 한다는 요구. 문항 하나에 근거 여러 개가 붙는다(1:N) —
    // 논문 하나로 시작해도 나중에 가이드라인·임상 자료가 붙는다.

    /** 드로어에서 문항을 골라 추가한다 — 문항 카드마다 폼을 두면 같은 폼이 문항 수만큼 반복된다. */
    @PostMapping("/admin/survey/evidence")
    public String addEvidence(@RequestParam String questionKey,
                              @RequestParam String title,
                              @RequestParam String rationale,
                              @RequestParam(required = false) String url,
                              @RequestParam(required = false) String sourceType,
                              @RequestParam(required = false) Integer sortOrder,
                              RedirectAttributes redirectAttributes) {
        try {
            evidenceService.add(questionKey, title, rationale, url, sourceType, sortOrder);
            redirectAttributes.addFlashAttribute("successMessage", "근거를 추가했습니다.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            // 실패하면 드로어를 다시 열어 준다 — 닫히면 사용자가 입력한 것이 어디로 갔는지 알 수 없다.
            redirectAttributes.addFlashAttribute("evidenceFormOpen", true);
        }
        return "redirect:/admin/survey";
    }

    @PostMapping("/admin/survey/evidence/{id}")
    public String updateEvidence(@PathVariable Long id,
                                 @RequestParam String title,
                                 @RequestParam String rationale,
                                 @RequestParam(required = false) String url,
                                 @RequestParam(required = false) String sourceType,
                                 @RequestParam(required = false) Integer sortOrder,
                                 RedirectAttributes redirectAttributes) {
        try {
            evidenceService.update(id, title, rationale, url, sourceType, sortOrder);
            redirectAttributes.addFlashAttribute("successMessage", "근거를 수정했습니다.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/survey";
    }

    @PostMapping("/admin/survey/evidence/{id}/delete")
    public String deleteEvidence(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        evidenceService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "근거를 삭제했습니다.");
        return "redirect:/admin/survey";
    }

    /**
     * survey 템플릿이 항상 필요로 하는 것.
     *
     * <p>🔴 {@code return "survey"} 가 다섯 군데 있다(정상 렌더 하나 + 검증 실패 네 군데).
     * 한 곳만 빠뜨리면 그 경로에서만 템플릿이 터지고, 화면에는 500 만 보인다 —
     * 정상 흐름은 멀쩡해서 테스트로도, 눈으로도 잘 안 걸린다.
     * 2026-09-08 근거 목록을 GET 에만 넣어 "중복 문항 키" 경로가 500 이 됐다.
     * 모델을 손으로 채우지 말고 이 메서드를 부를 것.
     */
    private void populateSurveyModel(Model model) {
        model.addAttribute("questions", service.getQuestions());
        model.addAttribute("evidenceByQuestion", evidenceService.byQuestion());
        model.addAttribute("sourceTypes", SOURCE_TYPES);
    }

    /** 마이그레이션의 CHECK 제약과 같은 집합. 화면의 select 가 이걸로 그려진다. */
    private static final java.util.List<String> SOURCE_TYPES =
            java.util.List.of("PAPER", "GUIDELINE", "CLINICAL", "ARTICLE", "INTERNAL");
}
