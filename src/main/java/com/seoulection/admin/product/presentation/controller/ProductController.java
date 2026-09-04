package com.seoulection.admin.product.presentation.controller;

import com.seoulection.admin.product.application.service.ProductService;
import com.seoulection.admin.product.domain.enums.ProductStage;
import com.seoulection.admin.product.domain.enums.ProductStatus;
import com.seoulection.admin.product.presentation.dto.ProductRegisterRequest;
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
import tools.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ProductController {

    /** 한 페이지에 보여 줄 건수. 표 한 화면에 들어오면서 스크롤이 과하지 않은 값. */
    private static final int PAGE_SIZE = 25;

    private final ProductService service;
    private final ObjectMapper objectMapper;

    public ProductController(ProductService service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    /**
     * 목록 화면. 기본 단계가 전체가 아니라 성분 보완인 이유: 이 화면을 여는 이유는 대개
     * "지금 내가 처리할 게 뭔가"이지 "전부 몇 개인가"가 아니다. 전체는 맨 끝에 둔다.
     *
     * <p>stage는 탭(작업 큐), status는 그 안의 단일 상태다. 함께 쓰면 정밀 조회가 된다 —
     * 예: {@code ?stage=ingredient-failed&status=NOT_FOUND}.
     */
    @GetMapping("/admin/products")
    public String page(@RequestParam(defaultValue = "ingredient-review") String stage,
                       @RequestParam(required = false) String status,
                       @RequestParam(required = false) String q,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        if (!model.containsAttribute("request")) {
            model.addAttribute("request", new ProductRegisterRequest());
        }
        populateProductList(model, stage, status, q, page);
        return "products";
    }

    /**
     * @param stageSlug 탭. 모르는 값이거나 "all"이면 상태 조건 없이 전체를 본다.
     * @param status    단계 안에서 한 상태만 보고 싶을 때. stage와 함께 쓴다.
     */
    private void populateProductList(Model model, String stageSlug, String status, String query, int page) {
        boolean searching = query != null && !query.isBlank();
        String keyword = searching ? query.trim() : null;
        ProductStage stage = ProductStage.from(stageSlug);
        ProductStatus exact = parseStatus(status);

        model.addAttribute("page", service.getProducts(statusesFor(stage, exact), keyword,
                Math.max(page, 0), PAGE_SIZE));
        model.addAttribute("stages", List.of(ProductStage.values()));
        model.addAttribute("selectedStage", stage);
        model.addAttribute("selectedStageSlug", stage == null ? "all" : stage.slug());
        model.addAttribute("selectedStatus", exact == null ? "" : exact.name());
        model.addAttribute("query", searching ? keyword : "");

        // 탭 배지·소계를 집계 질의 한 번으로 채운다. 검색 중이면 검색 결과 기준으로 센다 —
        // "이 브랜드 중 성분 보완이 몇 건인가"가 검색을 쓰는 이유이므로 전체 건수를 보이면 어긋난다.
        Map<ProductStatus, Long> byStatus = service.countByStatus(keyword);
        model.addAttribute("statusCounts", byStatus);
        model.addAttribute("stageCounts", stageCounts(byStatus));
        model.addAttribute("substatStatuses", stage == null ? List.of(ProductStatus.values()) : stage.statuses());
        model.addAttribute("productCount", byStatus.values().stream().mapToLong(Long::longValue).sum());
    }

    /**
     * 조회할 상태 집합. stage는 여러 상태를 묶은 작업 큐라서, 그 안의 한 상태만 보고 싶을 때가
     * 있다 — 예: 성분 확보 실패 단계에서 NOT_FOUND만. status 파라미터가 그 역할이다.
     * 그 단계에 속하지 않는 상태가 들어오면 무시하고 단계 전체를 보여 준다.
     */
    private List<ProductStatus> statusesFor(ProductStage stage, ProductStatus exact) {
        List<ProductStatus> inStage = stage == null ? List.of() : stage.statuses();
        if (exact == null) {
            return inStage;
        }
        if (inStage.isEmpty() || inStage.contains(exact)) {
            return List.of(exact);
        }
        return inStage;
    }

    private ProductStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return ProductStatus.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null; // 손으로 URL을 고친 경우다 — 목록을 깨뜨리지 말고 탭 전체를 보여 준다.
        }
    }

    /**
     * 단계별 건수. 키를 enum이 아니라 slug 문자열로 두는 이유: SpEL의 맵 조회가 enum 키를
     * 제대로 잡지 못해 템플릿에서 전부 null로 나온다. slug는 URL에도 그대로 쓰는 값이다.
     */
    private Map<String, Long> stageCounts(Map<ProductStatus, Long> byStatus) {
        Map<String, Long> stages = new LinkedHashMap<>();
        Arrays.stream(ProductStage.values()).forEach(stage -> stages.put(stage.slug(), 0L));
        Arrays.stream(ProductStatus.values())
                .forEach(status -> stages.merge(status.stage().slug(), byStatus.getOrDefault(status, 0L), Long::sum));
        return stages;
    }

    @PostMapping("/admin/products")
    public String register(
            @Valid @ModelAttribute("request") ProductRegisterRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            populateProductList(model, "ingredient-review", null, null, 0);
            model.addAttribute("registerFormOpen", true);
            return "products";
        }
        service.register(request.getName(), request.getBrand(), request.getCategory(),
                splitIngredients(request.getIngredientsText()));
        redirectAttributes.addFlashAttribute("successMessage", "제품을 등록했습니다.");
        return "redirect:/admin/products";
    }

    private List<String> splitIngredients(String text) {
        if (text == null || text.isBlank()) return List.of();
        return Arrays.stream(text.split("[,\\n]"))
                .map(String::trim).filter(value -> !value.isBlank()).distinct().toList();
    }

    /** 제품 상세. 파이프라인이 채운 값(카탈로그·언급·INCI API 원본)까지 전부 보여 준다. */
    @GetMapping("/admin/products/{id}")
    public String detail(@PathVariable String id, Model model) {
        var product = service.getProduct(id);
        model.addAttribute("product", product);
        model.addAttribute("inciapiRawJson", prettyJson(product.inciapiRawData()));
        return "product-detail";
    }

    private String prettyJson(Map<String, Object> raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(raw);
        } catch (RuntimeException e) {
            // 원본을 못 읽는다고 상세 화면 전체가 죽으면 안 된다 — 사유만 남기고 나머지를 보여 준다.
            return "원본 데이터를 표시할 수 없습니다: " + e.getMessage();
        }
    }

    @PostMapping("/admin/products/{id}/delete")
    public String delete(@PathVariable String id, RedirectAttributes redirectAttributes) {
        var product = service.getProduct(id);
        service.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "'" + product.name() + "'을(를) 삭제했습니다.");
        return "redirect:/admin/products";
    }

    /**
     * 1단계 저장 — 전성분만. 저장 후에는 방금까지 보던 성분 보완 큐로 돌려보낸다(다음 건을
     * 이어서 처리하는 흐름). 목록 첫 화면으로 튕기면 처리하던 자리를 잃는다.
     */
    @PostMapping("/admin/products/{id}/workflow/ingredients")
    public String workflowIngredients(@PathVariable String id, @ModelAttribute ProductRegisterRequest request,
                                    RedirectAttributes redirectAttributes) {
        boolean ingredientNotFound = "NOT_FOUND".equals(request.getIngredientResolution());
        List<String> ingredients = splitIngredients(request.getIngredientsText());
        if (ingredientNotFound && !ingredients.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "성분을 찾지 못함을 선택한 경우 성분 입력란을 비워 주세요.");
            return "redirect:/admin/products/" + id + "/workflow";
        }
        if (!ingredientNotFound && ingredients.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "성분을 입력하거나, 찾지 못한 경우 '성분을 찾지 못함'을 선택해 주세요.");
            return "redirect:/admin/products/" + id + "/workflow";
        }
        service.reviewIngredients(id, ingredients, ingredientNotFound);
        redirectAttributes.addFlashAttribute("successMessage", "전성분을 저장했습니다.");
        return "redirect:/admin/products?stage=ingredient-review";
    }

    /** 2단계 저장 — 식약처 기능성만. 저장 후 기능성 확인 큐로 돌아간다. */
    @PostMapping("/admin/products/{id}/workflow/functions")
    public String workflowFunctions(@PathVariable String id, @ModelAttribute ProductRegisterRequest request,
                                 RedirectAttributes redirectAttributes) {
        String result = request.getFunctionResult();
        if (result == null || result.isBlank()) {
            redirectAttributes.addFlashAttribute("errorMessage", "의약품안전나라 조회 결과를 선택해 주세요.");
            return "redirect:/admin/products/" + id + "/workflow";
        }
        boolean confirmed = "CONFIRMED".equals(result);
        boolean hasFunction = !request.getFunction().isEmpty();
        if (confirmed && !hasFunction) {
            redirectAttributes.addFlashAttribute("errorMessage", "기능성 확인을 선택한 경우 유형을 최소 1개 선택해 주세요.");
            return "redirect:/admin/products/" + id + "/workflow";
        }
        // '기능성 아님'을 고르고 유형을 남겨 두면 모순이므로 유형을 버린다.
        service.reviewFunction(id, confirmed ? request.getFunction() : List.of());
        redirectAttributes.addFlashAttribute("successMessage", "기능성 검수 정보를 저장했습니다.");
        return "redirect:/admin/products?stage=functional-review";
    }

    /**
     * 검수 작업 화면. 어느 단계를 열지는 제품 상태가 정한다({@link ProductStatus#workflowStep()}).
     * step 파라미터는 이미 지나간 단계를 다시 여는 용도다 — 기능성 화면에서 "성분 수정"으로
     * 넘어가거나, 파이프라인이 늦어 PENDING 제품에 성분을 직접 넣는 경우.
     */
    @GetMapping("/admin/products/{id}/workflow")
    public String workflowPage(@PathVariable String id,
                             @RequestParam(required = false) String step,
                             Model model) {
        var product = service.getProduct(id);
        ProductRegisterRequest request = new ProductRegisterRequest();
        request.setName(product.name());
        request.setBrand(product.brand());
        request.setCategory(product.category());
        request.setIngredientResolution(product.status() == ProductStatus.NOT_FOUND ? "NOT_FOUND" : "FOUND");
        request.setFunction(product.function().stream().map(Enum::name).toList());
        // 이미 검수한 제품만 현재 값을 찍어 준다. 미검수면 비워 둬서 어드민이 직접 고르게 한다.
        if (product.status().functionalReviewDone()) {
            request.setFunctionResult(product.hasFunction() ? "CONFIRMED" : "NONE");
        }
        request.setIngredientsText(product.ingredients() == null ? "" : String.join(", ", product.ingredients()));
        model.addAttribute("product", product);
        model.addAttribute("request", request);
        String resolved = "ingredients".equals(step) || "functional".equals(step)
                ? step
                : product.status().workflowStep();
        if (resolved == null) {
            // 파이프라인이 굴리는 중이거나 이미 끝난 제품은 어드민이 할 일이 없다 — 상세로 보낸다.
            return "redirect:/admin/products/" + id;
        }
        model.addAttribute("workflowStep", resolved);
        return "product-workflow";
    }
}
