package com.seoulection.admin.product.functional.presentation;

import com.seoulection.admin.product.functional.application.FunctionalScreeningQueueSettings;
import com.seoulection.admin.product.functional.application.FunctionalScreeningService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/functional-screening-settings")
public class FunctionalScreeningSettingsController {
    private final FunctionalScreeningQueueSettings settings;
    private final FunctionalScreeningService screening;
    public FunctionalScreeningSettingsController(FunctionalScreeningQueueSettings settings, FunctionalScreeningService screening) {
        this.settings = settings; this.screening = screening;
    }
    @GetMapping
    public String page(Model model) {
        return "redirect:/admin/products?stage=functional-review&tab=settings";
    }
    @PostMapping
    public String update(@RequestParam(defaultValue="false") boolean autoEnqueue,
                         @RequestParam int scanIntervalMinutes, @RequestParam int capacity,
                         RedirectAttributes attributes) {
        try {
            settings.update(autoEnqueue, scanIntervalMinutes, capacity);
            attributes.addFlashAttribute("successMessage", "설정을 저장했습니다. 재시작 없이 다음 확인 주기에 반영됩니다.");
        } catch (IllegalArgumentException e) {
            attributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/products?stage=functional-review&tab=settings";
    }
}
