package com.seoulection.admin.ingredient.presentation.controller;

import com.seoulection.admin.ingredient.infrastructure.repository.IngredientPostgresRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PropertyDefinitionController {
    private final IngredientPostgresRepository repository;
    public PropertyDefinitionController(IngredientPostgresRepository repository) { this.repository = repository; }

    @GetMapping("/admin/ingredient-properties")
    public String page(Model model) {
        model.addAttribute("definitions", repository.findPropertyDefinitions());
        return "ingredient-properties";
    }

    @PostMapping("/admin/ingredient-properties")
    public String save(@RequestParam String propertyKey, @RequestParam String displayNameKo,
                       @RequestParam String valueType, @RequestParam(required = false) String valueUnit,
                       @RequestParam(required = false) String description, RedirectAttributes redirect) {
        repository.savePropertyDefinition(propertyKey, displayNameKo, valueType, valueUnit, description);
        redirect.addFlashAttribute("successMessage", "특성 정의를 저장했습니다.");
        return "redirect:/admin/ingredient-properties";
    }

    @PostMapping("/admin/ingredient-properties/{key}/delete")
    public String delete(@PathVariable String key, RedirectAttributes redirect) {
        try {
            repository.deletePropertyDefinition(key);
            redirect.addFlashAttribute("successMessage", "특성 정의를 삭제했습니다.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/ingredient-properties";
    }
}
