package com.seoulection.admin.ingredient.presentation.controller;

import com.seoulection.admin.ingredient.application.service.IngredientService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.seoulection.admin.ingredient.presentation.dto.IngredientCreateRequest;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;

@Controller
public class IngredientController {
    private final IngredientService service;
    public IngredientController(IngredientService service) { this.service = service; }

    @GetMapping("/admin/ingredients")
    public String page(Model model) {
        if (!model.containsAttribute("request")) model.addAttribute("request", new IngredientCreateRequest());
        model.addAttribute("ingredients", service.getIngredients());
        model.addAttribute("propertyDefinitions", service.getPropertyDefinitions());
        return "ingredients";
    }

    @PostMapping("/admin/ingredients")
    public String create(@Valid @ModelAttribute("request") IngredientCreateRequest request,
                         BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            // 등록 폼은 기본으로 접혀 있다 — 검증에 실패했으면 펼쳐서 오류를 보여 줘야 한다.
            model.addAttribute("ingredients", service.getIngredients());
            model.addAttribute("propertyDefinitions", service.getPropertyDefinitions());
            model.addAttribute("registerFormOpen", true);
            return "ingredients";
        }
        service.create(request.getInciName(), request.getDisplayNameKo(), request.getFamily(),
                request.getAliasesText(), request.getEffectsText(), request.getPropertiesText(),
                request.getEvidenceText(), request.getEfficacyRangesText(), request.getEfficacyConditionsText());
        redirectAttributes.addFlashAttribute("successMessage", "성분을 등록했습니다.");
        return "redirect:/admin/ingredients";
    }

    @GetMapping("/admin/ingredients/{id}/review")
    public String reviewPage(@PathVariable String id, Model model) {
        var ingredient = service.getIngredient(id);
        IngredientCreateRequest request = new IngredientCreateRequest();
        request.setInciName(ingredient.getInciName());
        request.setDisplayNameKo(ingredient.getDisplayNameKo());
        request.setFamily(ingredient.getFamily());
        request.setAliasesText(String.join(", ", ingredient.getAliases()));
        request.setEffectsText(ingredient.getEffects().entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue()).collect(java.util.stream.Collectors.joining(", ")));
        request.setPropertiesText(ingredient.getProperties().entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue()).collect(java.util.stream.Collectors.joining(", ")));
        request.setEvidenceText(ingredient.getEvidences().stream()
                .map(e -> String.join("|", java.util.Objects.toString(e.sourceType(), ""), java.util.Objects.toString(e.title(), ""),
                        java.util.Objects.toString(e.url(), ""), Boolean.toString(e.humanEvidence()), java.util.Objects.toString(e.evidenceLevel(), ""),
                        java.util.Objects.toString(e.targetScore(), ""), Boolean.toString(e.ingredientSpecific()), java.util.Objects.toString(e.productForm(), ""),
                        java.util.Objects.toString(e.studyConcentration(), ""), java.util.Objects.toString(e.studyConcentrationUnit(), ""), java.util.Objects.toString(e.summary(), "")))
                .collect(java.util.stream.Collectors.joining("\n")));
        request.setEfficacyRangesText(ingredient.getEfficacyRanges().stream()
                .map(e -> String.join("|", java.util.Objects.toString(e.targetKey(), ""), java.util.Objects.toString(e.productType(), ""),
                        java.util.Objects.toString(e.concentrationMin(), ""), java.util.Objects.toString(e.concentrationMax(), ""), java.util.Objects.toString(e.concentrationUnit(), ""),
                        java.util.Objects.toString(e.onsetConcentration(), ""), java.util.Objects.toString(e.irritationConcentration(), ""),
                        java.util.Objects.toString(e.evidenceId(), ""), java.util.Objects.toString(e.notes(), "")))
                .collect(java.util.stream.Collectors.joining("\n")));
        request.setEfficacyConditionsText(ingredient.getEfficacyRanges().stream()
                .flatMap(r -> r.conditions().stream())
                .map(c -> String.join("|", c.targetKey(), c.parameterKey(), java.util.Objects.toString(c.valueMin(), ""), java.util.Objects.toString(c.valueMax(), ""), java.util.Objects.toString(c.valueUnit(), ""), java.util.Objects.toString(c.valueText(), ""), java.util.Objects.toString(c.conditionMode(), "EXACT_VALUE"), Boolean.toString(c.interpolationAllowed())))
                .collect(java.util.stream.Collectors.joining("\n")));
        model.addAttribute("ingredient", ingredient);
        model.addAttribute("request", request);
        model.addAttribute("propertyDefinitions", service.getPropertyDefinitions());
        return "ingredient-review";
    }

    @PostMapping("/admin/ingredients/{id}/review")
    public String review(@PathVariable String id, @Valid @ModelAttribute("request") IngredientCreateRequest request,
                         BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("ingredient", service.getIngredient(id));
            return "ingredient-review";
        }
        service.update(id, request.getInciName(), request.getDisplayNameKo(), request.getFamily(),
                request.getAliasesText(), request.getEffectsText(), request.getPropertiesText(),
                request.getEvidenceText(), request.getEfficacyRangesText(), request.getEfficacyConditionsText());
        redirectAttributes.addFlashAttribute("successMessage", "성분 검수 정보를 저장했습니다.");
        return "redirect:/admin/ingredients";
    }
}
