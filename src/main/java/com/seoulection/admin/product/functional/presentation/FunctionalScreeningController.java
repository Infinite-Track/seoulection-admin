package com.seoulection.admin.product.functional.presentation;

import com.seoulection.admin.product.functional.application.FunctionalScreeningService;
import com.seoulection.admin.product.functional.domain.FunctionalScreening;
import com.seoulection.admin.product.functional.domain.ScreeningOutcome;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * 자동 조회를 사람이 직접 돌리는 자리. 두 가지뿐이다 — 이 제품 다시 조회, 큐 한 번에 조회.
 *
 * <p>여기서 기능성을 저장하지는 않는다. 저장은 기존 {@code /workflow/functions} 폼이 계속
 * 맡는다 — 자동 조회는 <b>사람이 고를 재료를 채워 주는 일</b>이고, 규제 정보를 확정하는
 * 마지막 클릭은 사람 몫으로 남긴다.
 */
@Controller
public class FunctionalScreeningController {

    /** 한 번에 훑을 상한. 안전나라가 제품당 여러 번 불려서 무제한으로 돌리면 쿼터가 먼저 나간다. */
    private static final int DEFAULT_BATCH_LIMIT = 50;

    private final FunctionalScreeningService service;

    public FunctionalScreeningController(FunctionalScreeningService service) {
        this.service = service;
    }

    @PostMapping("/admin/products/{id}/functional-screening")
    public String rescreen(@PathVariable String id, RedirectAttributes redirectAttributes) {
        FunctionalScreening screening = service.screen(id);
        redirectAttributes.addFlashAttribute("successMessage",
                "자동 조회를 다시 실행했습니다 — " + screening.outcome().displayName() + ": " + screening.reason());
        return "redirect:/admin/products/" + id + "/workflow?step=functional";
    }

    @PostMapping("/admin/products/functional-screening")
    public String rescreenQueue(@RequestParam(required = false) Integer limit,
                                RedirectAttributes redirectAttributes) {
        Map<ScreeningOutcome, Integer> summary = service.screenQueue(limit == null ? DEFAULT_BATCH_LIMIT : limit);
        String detail = summary.isEmpty() ? "처리할 제품이 없습니다"
                : summary.entrySet().stream()
                        .map(entry -> entry.getKey().displayName() + " " + entry.getValue() + "건")
                        .collect(Collectors.joining(", "));
        redirectAttributes.addFlashAttribute("successMessage", "기능성 자동 조회 완료 — " + detail);
        return "redirect:/admin/products?stage=functional-review";
    }
}
