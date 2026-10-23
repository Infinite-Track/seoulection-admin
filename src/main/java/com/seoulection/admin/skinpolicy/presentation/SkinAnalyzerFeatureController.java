package com.seoulection.admin.skinpolicy.presentation;

import com.seoulection.admin.skinpolicy.application.SkinAnalyzerFeatureService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/** 분석기별 사진 신뢰 지표 화면. 수정은 분석기 한 행씩 POST(폼은 GET/POST 만 보낸다). */
@Controller
public class SkinAnalyzerFeatureController {

    private final SkinAnalyzerFeatureService service;

    public SkinAnalyzerFeatureController(SkinAnalyzerFeatureService service) {
        this.service = service;
    }

    @GetMapping("/admin/skin-analyzer-features")
    public String page(Model model) {
        model.addAttribute("features", service.features());
        model.addAttribute("analyzers", service.analyzers());
        return "skin-analyzer-features";
    }

    /** 체크된 지표 집합으로 통째로 바꾼다. 하나도 안 보내면(전부 해제) 행이 없어진다. */
    @PostMapping("/admin/skin-analyzer-features/{analyzer}")
    public String replace(@PathVariable String analyzer,
                          @RequestParam(name = "featureKeys", required = false) List<String> featureKeys,
                          RedirectAttributes redirectAttributes) {
        try {
            service.replace(analyzer, featureKeys == null ? List.of() : featureKeys);
            redirectAttributes.addFlashAttribute("successMessage",
                    analyzer + " 의 사진 신뢰 지표를 저장했습니다. 다음 분석·문항 조회부터 반영됩니다.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/skin-analyzer-features";
    }

    /** 새 분석기 행 추가 — 이름만 받고 지표는 비운 채 만든다(그 뒤 체크해서 저장). */
    @PostMapping("/admin/skin-analyzer-features")
    public String add(@RequestParam String analyzer,
                      @RequestParam(name = "featureKeys", required = false) List<String> featureKeys,
                      RedirectAttributes redirectAttributes) {
        return replace(analyzer, featureKeys, redirectAttributes);
    }
}
