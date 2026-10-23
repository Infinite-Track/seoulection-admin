package com.seoulection.admin.demo.presentation;

import com.seoulection.admin.demo.application.DemoFaceService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.Map;

/** 데모 얼굴 관리 화면. 점수 입력은 {@code score[feature_key]} 이름의 숫자 칸으로 받는다. */
@Controller
public class DemoFaceController {

    private static final String SCORE_PREFIX = "score[";

    private final DemoFaceService service;

    public DemoFaceController(DemoFaceService service) {
        this.service = service;
    }

    @GetMapping("/admin/demo-faces")
    public String page(Model model) {
        model.addAttribute("features", service.features());
        model.addAttribute("profiles", service.list());
        return "demo-faces";
    }

    @PostMapping("/admin/demo-faces")
    public String create(@RequestParam String code, @RequestParam String label, @RequestParam String imageUrl,
                         @RequestParam(defaultValue = "0") int sortOrder, RedirectAttributes redirectAttributes) {
        try {
            service.create(code, label, imageUrl, sortOrder);
            redirectAttributes.addFlashAttribute("successMessage", "데모 얼굴을 추가했습니다. 점수는 전부 0.75로 시작합니다.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/demo-faces";
    }

    @PostMapping("/admin/demo-faces/{code}")
    public String update(@PathVariable String code, @RequestParam String label, @RequestParam String imageUrl,
                         @RequestParam(defaultValue = "0") int sortOrder,
                         @RequestParam(defaultValue = "false") boolean active,
                         @RequestParam Map<String, String> params,
                         RedirectAttributes redirectAttributes) {
        try {
            Map<String, Double> scores = new LinkedHashMap<>();
            for (var e : params.entrySet()) {
                if (e.getKey().startsWith(SCORE_PREFIX) && e.getKey().endsWith("]") && !e.getValue().isBlank()) {
                    String featureKey = e.getKey().substring(SCORE_PREFIX.length(), e.getKey().length() - 1);
                    scores.put(featureKey, Double.parseDouble(e.getValue()));
                }
            }
            service.update(code, label, imageUrl, sortOrder, active, scores);
            redirectAttributes.addFlashAttribute("successMessage", code + " 를 저장했습니다. 다음 데모 추천부터 반영됩니다.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/demo-faces";
    }
}
