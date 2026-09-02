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
        return "ingredients";
    }

    @PostMapping("/admin/ingredients")
    public String create(@Valid @ModelAttribute("request") IngredientCreateRequest request,
                         BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            // 등록 폼은 기본으로 접혀 있다 — 검증에 실패했으면 펼쳐서 오류를 보여 줘야 한다.
            model.addAttribute("ingredients", service.getIngredients());
            model.addAttribute("registerFormOpen", true);
            return "ingredients";
        }
        service.create(request.getCanonicalName(), request.getInciName(), request.getDisplayNameKo(), request.getFamily(),
                request.getAliasesText(), request.getSearchGroupsText(), request.getEffectsText());
        redirectAttributes.addFlashAttribute("successMessage", "성분을 등록했습니다.");
        return "redirect:/admin/ingredients";
    }

    @GetMapping("/admin/ingredients/{id}/review")
    public String reviewPage(@PathVariable String id, Model model) {
        var ingredient = service.getIngredient(id);
        IngredientCreateRequest request = new IngredientCreateRequest();
        request.setCanonicalName(ingredient.getCanonicalName());
        request.setInciName(ingredient.getInciName());
        request.setDisplayNameKo(ingredient.getDisplayNameKo());
        request.setFamily(ingredient.getFamily());
        request.setAliasesText(String.join(", ", ingredient.getAliases()));
        request.setSearchGroupsText(String.join(", ", ingredient.getSearchGroups()));
        request.setEffectsText(ingredient.getEffects().entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue()).collect(java.util.stream.Collectors.joining(", ")));
        model.addAttribute("ingredient", ingredient);
        model.addAttribute("request", request);
        return "ingredient-review";
    }

    @PostMapping("/admin/ingredients/{id}/review")
    public String review(@PathVariable String id, @Valid @ModelAttribute("request") IngredientCreateRequest request,
                         BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("ingredient", service.getIngredient(id));
            return "ingredient-review";
        }
        service.update(id, request.getCanonicalName(), request.getInciName(), request.getDisplayNameKo(), request.getFamily(),
                request.getAliasesText(), request.getSearchGroupsText(), request.getEffectsText());
        redirectAttributes.addFlashAttribute("successMessage", "성분 검수 정보를 저장했습니다.");
        return "redirect:/admin/ingredients";
    }
}
