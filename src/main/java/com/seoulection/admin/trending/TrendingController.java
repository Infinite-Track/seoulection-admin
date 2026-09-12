package com.seoulection.admin.trending;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;

@Controller
@RequestMapping("/admin/trending")
public class TrendingController {
    private final TrendingService service;
    public TrendingController(TrendingService service){this.service=service;}
    @GetMapping public String page(@RequestParam(defaultValue = "") String productQuery, Model model){
        populatePage(model, productQuery);
        model.addAttribute("settings",service.activeSettings());
        return "trending-policy";
    }
    @PostMapping("/preview") public String preview(@ModelAttribute TrendingService.Settings settings,Model model){populatePage(model, "");model.addAttribute("settings",settings);model.addAttribute("results",service.preview(settings));return "trending-policy";}
    @PostMapping("/apply") public String apply(@ModelAttribute TrendingService.Settings settings, RedirectAttributes flash){service.apply(settings,"admin");flash.addFlashAttribute("successMessage","새 트렌딩 기준을 계산해 사용자 홈에 적용했습니다.");return "redirect:/admin/trending";}
    @PostMapping("/oliveyoung") public String oliveyoung(@RequestParam String productId,@RequestParam int rank,@RequestParam LocalDate measuredAt,RedirectAttributes flash){service.saveOliveyoung(productId,rank,measuredAt);flash.addFlashAttribute("successMessage","올리브영 순위를 저장했습니다.");return "redirect:/admin/trending";}

    private void populatePage(Model model, String productQuery) {
        model.addAttribute("signals", service.oliveyoungSignals());
        model.addAttribute("productQuery", productQuery);
        model.addAttribute("productResults", service.searchProducts(productQuery));
        model.addAttribute("today", LocalDate.now());
    }
}
