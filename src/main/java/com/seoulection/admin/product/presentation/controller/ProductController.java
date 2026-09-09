package com.seoulection.admin.product.presentation.controller;

import com.seoulection.admin.product.application.service.ProductService;
import com.seoulection.admin.product.domain.enums.ProductFunctionalCategory;
import com.seoulection.admin.product.functional.application.FunctionalScreeningService;
import com.seoulection.admin.product.functional.domain.FunctionalScreening;
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
import com.seoulection.admin.product.application.dto.ProductIngredientProperty;
import java.util.ArrayList;
import java.math.BigDecimal;
import com.seoulection.admin.product.infrastructure.repository.PurchaseLinkRepository;

@Controller
public class ProductController {

    /** 한 페이지에 보여 줄 건수. 표 한 화면에 들어오면서 스크롤이 과하지 않은 값. */
    private static final int PAGE_SIZE = 25;

    private final ProductService service;
    private final FunctionalScreeningService screeningService;
    private final ObjectMapper objectMapper;
    private final PurchaseLinkRepository purchaseLinkRepository;

    public ProductController(ProductService service, FunctionalScreeningService screeningService,
                             ObjectMapper objectMapper, PurchaseLinkRepository purchaseLinkRepository) {
        this.service = service;
        this.screeningService = screeningService;
        this.objectMapper = objectMapper;
        this.purchaseLinkRepository = purchaseLinkRepository;
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
        List<String> ingredients = splitIngredients(request.getIngredientsText());
        var created = service.register(request.getName(), request.getNameKo(), request.getBrand(),
                request.getCategory(), ingredients);
        // 성분을 함께 넣었으면 함량을 바로 채우게 성분 탭으로 보낸다. 함량은 성분 행이
        // 저장된 뒤에야 붙일 수 있어 등록 폼에서 미리 받을 수 없다.
        if (!ingredients.isEmpty()) {
            redirectAttributes.addFlashAttribute("successMessage", "제품을 등록했습니다. 이어서 함량을 입력하세요.");
            return "redirect:/admin/products/" + created.id() + "/workflow?step=ingredients";
        }
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
        model.addAttribute("productIngredients", service.getProductIngredients(id));
        model.addAttribute("propertyDefinitions", service.propertyDefinitions());
        model.addAttribute("inciapiRawJson", prettyJson(product.inciapiRawData()));
        model.addAttribute("purchaseLinks", purchaseLinkRepository.find(id));
        return "product-detail";
    }

    @PostMapping("/admin/products/{id}/purchase-links")
    public String addPurchaseLink(@PathVariable String id, @RequestParam String url,
                                  @RequestParam(required=false) String domain,
                                  @RequestParam(required=false) String region,
                                  RedirectAttributes redirectAttributes) {
        if (url == null || url.isBlank()) { redirectAttributes.addFlashAttribute("errorMessage", "구매 링크를 입력하세요."); }
        else { purchaseLinkRepository.add(id, url.trim(), domain == null || domain.isBlank() ? domainFrom(url) : domain.trim(), region); redirectAttributes.addFlashAttribute("successMessage", "구매 링크를 추가했습니다."); }
        return "redirect:/admin/products/" + id;
    }

    @PostMapping("/admin/products/{id}/purchase-links/{linkId}/delete")
    public String deletePurchaseLink(@PathVariable String id, @PathVariable long linkId, RedirectAttributes redirectAttributes) {
        purchaseLinkRepository.delete(id, linkId); redirectAttributes.addFlashAttribute("successMessage", "구매 링크를 삭제했습니다."); return "redirect:/admin/products/" + id;
    }

    private String domainFrom(String value) { try { return java.net.URI.create(value).getHost(); } catch (RuntimeException e) { return ""; } }

    @PostMapping("/admin/products/{id}/basic")
    public String updateBasic(@PathVariable String id, @RequestParam String name,
                              @RequestParam(required=false) String nameKo, @RequestParam String brand,
                              @RequestParam String category, RedirectAttributes redirectAttributes) {
        service.updateBasicInfo(id, name, nameKo, brand, category);
        redirectAttributes.addFlashAttribute("successMessage", "제품 기본 정보를 저장했습니다.");
        return "redirect:/admin/products/" + id;
    }

    /**
     * 성분 한 행 저장 — 함량과 <b>이 제품에서의 특성</b>.
     *
     * <p>특성은 폼에서 {@code propertyKey[]}, {@code propertyValueText[]} ... 처럼 같은 이름의 배열로
     * 온다. 순서가 곧 짝이므로 인덱스로 묶는다. 빈 칸은 저장하지 않는다 — 지운 것과 같게 다룬다.
     */
    @PostMapping("/admin/products/{id}/ingredients/{rowId}")
    public String reviewIngredient(@PathVariable String id, @PathVariable long rowId,
            @RequestParam(required=false) String ingredientId,
            @RequestParam(required=false) BigDecimal concentrationMin,
            @RequestParam(required=false) BigDecimal concentrationMax,
            @RequestParam(required=false) String unit,
            @RequestParam(required=false) String notes,
            @RequestParam(required=false) List<String> propertyKey,
            @RequestParam(required=false) List<String> propertyValueText,
            @RequestParam(required=false) List<String> propertyValueMin,
            @RequestParam(required=false) List<String> propertyValueMax,
            RedirectAttributes redirectAttributes) {
        service.reviewProductIngredient(id, rowId, ingredientId, concentrationMin, concentrationMax, unit, notes,
                toProperties(propertyKey, propertyValueText, propertyValueMin, propertyValueMax));
        redirectAttributes.addFlashAttribute("successMessage", "제품별 성분 정보를 저장했습니다.");
        return "redirect:/admin/products/" + id;
    }

    private List<ProductIngredientProperty> toProperties(List<String> keys, List<String> texts,
                                                        List<String> mins, List<String> maxs) {
        if (keys == null) return List.of();   // 특성 칸이 아예 없는 폼 → 건드리지 않는다
        List<ProductIngredientProperty> properties = new ArrayList<>();
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            if (key == null || key.isBlank()) continue;
            properties.add(new ProductIngredientProperty(key, at(texts, i),
                    decimal(at(mins, i)), decimal(at(maxs, i)), null, null));
        }
        return properties;
    }

    private String at(List<String> values, int index) {
        return values == null || index >= values.size() ? null : values.get(index);
    }

    /** 빈 칸과 "숫자가 아님"을 모두 null 로 본다 — 폼 하나 때문에 500을 내지 않는다. */
    private BigDecimal decimal(String value) {
        if (value == null || value.isBlank()) return null;
        try { return new BigDecimal(value.trim()); } catch (NumberFormatException e) { return null; }
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
        // 성분을 찾지 못했으면 채울 함량이 없으니 큐로 돌아간다. 찾았으면 같은 탭에 남아
        // 방금 저장된 성분 목록에 함량을 채우게 한다 — 화면을 옮기면 맥락이 끊긴다.
        if (ingredientNotFound) {
            redirectAttributes.addFlashAttribute("successMessage", "성분을 찾지 못함으로 저장했습니다.");
            return "redirect:/admin/products?stage=ingredient-review";
        }
        redirectAttributes.addFlashAttribute("successMessage",
                "전성분을 저장했습니다. 이어서 성분별 보완을 마치고 아래 '성분 보완 완료'를 누르세요.");
        return "redirect:/admin/products/" + id + "/workflow?step=ingredients";
    }

    /**
     * 1단계 완료 선언 — 성분별 보완을 마쳤다는 뜻이고, 여기서 상태가 INGREDIENTS_ADDED 가 된다.
     *
     * <p>함량을 하나도 안 채웠어도 누를 수 있다. 채울 값이 없는 제품이 실제로 있고, 그때
     * 완료를 막으면 제품이 성분 보완 큐에 영원히 남는다.
     */
    @PostMapping("/admin/products/{id}/workflow/ingredients/complete")
    public String completeIngredientReview(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            service.completeIngredientReview(id);
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/products/" + id + "/workflow?step=ingredients";
        }
        redirectAttributes.addFlashAttribute("successMessage", "성분 보완을 마쳤습니다. 이어서 기능성을 확인하세요.");
        return "redirect:/admin/products/" + id + "/workflow?step=functional";
    }

    /**
     * 2단계 자동 — 한글 이름을 저장하고 곧바로 의약품안전나라를 조회해 기능성까지 기록한다.
     *
     * <p>트리거가 한글 이름인 이유: 안전나라 등록명(ITEM_NAME)은 전부 한글이고 브랜드 한글
     * 표기로 시작한다("구달청귤비타씨잡티세럼"). 영문 제품명만으로는 조회가 시작조차 안 되므로,
     * 한글 이름이 채워지는 그 순간이 자동 조회가 가장 잘 듣는 시점이다.
     *
     * <p>확정되면 큐로 돌아가고, 못 찾으면 같은 화면에 남아 후보와 사유를 보여 준다 —
     * 어드민이 손대는 건 그때뿐이다.
     */
    @PostMapping("/admin/products/{id}/workflow/functional-screening")
    public String workflowScreen(@PathVariable String id, @RequestParam(required = false) String nameKo,
                                 RedirectAttributes redirectAttributes) {
        if (nameKo == null || nameKo.isBlank()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "자동 조회는 한글 이름으로 검색합니다 — 한글 이름을 먼저 입력해 주세요.");
            return "redirect:/admin/products/" + id + "/workflow?step=functional";
        }
        var current = service.getProduct(id);
        service.updateBasicInfo(id, current.name(), nameKo.trim(), current.brand(), current.category());

        var screened = screeningService.screenAfterNameSaved(id);
        if (screened.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "자동 조회가 꺼져 있습니다(admin.functional-screening.enabled). 아래에서 직접 입력해 주세요.");
            return "redirect:/admin/products/" + id + "/workflow?step=functional";
        }

        FunctionalScreening screening = screened.get();
        if (screening.outcome().decided() && screeningService.appliesDecisions()) {
            // 규제 정보가 조용히 저장되고 화면만 넘어가면 나중에 되짚을 실마리가 없다 —
            // 무엇이 어떤 근거로 기록됐는지 문구로 남긴다.
            redirectAttributes.addFlashAttribute("successMessage",
                    "한글 이름을 저장하고 기능성을 자동 확정했습니다 — " + describe(screening));
            return "redirect:/admin/products?stage=functional-review";
        }
        if (screening.outcome().decided()) {
            // 판정은 끝났지만 확정은 사람이 한다. 폼이 미리 채워진 채로 열리고, 어드민은
            // 근거를 보고 저장만 누르면 된다.
            redirectAttributes.addFlashAttribute("successMessage",
                    "자동 조회 결과를 아래에 채워 두었습니다 — " + describe(screening)
                            + " 확인 후 저장을 눌러 확정해 주세요.");
            return "redirect:/admin/products/" + id + "/workflow?step=functional";
        }
        redirectAttributes.addFlashAttribute("errorMessage",
                "자동 조회로 확정하지 못했습니다(" + screening.outcome().displayName() + "): "
                        + screening.reason() + " 아래에서 직접 확인해 주세요.");
        return "redirect:/admin/products/" + id + "/workflow?step=functional";
    }

    /**
     * 자동 판정 결과를 검수 폼에 미리 채운다. 어드민은 근거를 보고 저장만 누르면 된다.
     *
     * <p>⚠️ <b>"기능성 아님"은 미리 고르지 않는다.</b> 확인 없이 저장만 눌러도 식약처 기능성이
     * 아니라는 사실이 기록되는 게 이 폼에서 가장 비싼 실수이고, 자동 조회가 못 찾은 것과
     * 실제로 기능성이 아닌 것은 겉보기가 같다. 반대로 유형이 나온 경우는 안전나라 응답이라는
     * 근거가 있으므로 채워 둔다.
     *
     * <p>이미 검수를 마친 제품은 건드리지 않는다 — 사람이 정한 값을 자동 판정이 덮으면 안 된다.
     */
    private void prefillFromScreening(ProductRegisterRequest request,
                                      com.seoulection.admin.product.application.dto.ProductResult product,
                                      FunctionalScreening screening) {
        if (screening == null || product.status().functionalReviewDone() || screening.claims().isEmpty()) {
            return;
        }
        request.setFunctionResult("CONFIRMED");
        request.setFunction(screening.claims().stream().map(Enum::name).toList());
    }

    /** 자동 확정 결과 문구. 유형이 비어 있으면 "기능성 아님"으로 확정된 것이다. */
    private String describe(FunctionalScreening screening) {
        var selected = screening.selected();
        String evidence = selected == null ? "" : " / 근거: " + selected.item().itemName();
        if (screening.claims().isEmpty()) {
            return "기능성 아님 (" + screening.reason() + ")";
        }
        return screening.claims().stream()
                .map(ProductFunctionalCategory::displayName)
                .reduce((a, b) -> a + ", " + b).orElse("") + evidence;
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
        // 한글 이름은 기능성과 같은 탭에서 받는다. 비워 두면 유지한다 —
        // 지우려는 의도와 구분할 수 없어 덮어쓰지 않는다.
        String nameKo = request.getNameKo();
        if (nameKo != null && !nameKo.isBlank()) {
            var current = service.getProduct(id);
            service.updateBasicInfo(id, current.name(), nameKo.trim(), current.brand(), current.category());
        }
        boolean confirmed = "CONFIRMED".equals(result);
        boolean hasFunction = !request.getFunction().isEmpty();
        if (confirmed && !hasFunction) {
            redirectAttributes.addFlashAttribute("errorMessage", "기능성 확인을 선택한 경우 유형을 최소 1개 선택해 주세요.");
            return "redirect:/admin/products/" + id + "/workflow";
        }
        // '기능성 아님'을 고르고 유형을 남겨 두면 모순이므로 유형을 버린다.
        service.reviewFunction(id, confirmed ? request.getFunction() : List.of());
        // 자동 판정이 남아 있다면 "사람이 정했다"로 덮는다 — 나중에 이 제품의 기능성이
        // 누구의 판단이었는지 되짚을 수 있어야 한다.
        screeningService.markDecidedByAdmin(id);
        redirectAttributes.addFlashAttribute("successMessage", "기능성 검수 정보를 저장했습니다.");
        return "redirect:/admin/products?stage=functional-review";
    }

    /**
     * 검수 탭. 기존 2탭 구조를 지킨다 — 성분 보완(성분+함량)과 기능성 확인(한글 이름+기능성).
     *
     * <p>탭을 더 쪼개지 않는 이유: 근거 자료가 다른 두 작업이라 나누는 것이지, 입력 항목마다
     * 나누면 저장 버튼만 늘고 어드민이 같은 제품을 네 번 열게 된다.
     */
    private static final List<String> WORKFLOW_STEPS = List.of("ingredients", "functional");

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
        // 4단계 마법사: 성분 → 함량 → 한글 이름 → 기능성.
        // step 이 없으면 제품 상태가 진입점을 정한다(status.workflowStep()).
        // ⚠️ step == null 검사를 빼지 말 것. List.of() 는 불변 리스트라 contains(null) 이
        //    false 가 아니라 NullPointerException 이다(step 파라미터는 대개 없다).
        String resolved = step != null && WORKFLOW_STEPS.contains(step)
                ? step : product.status().workflowStep();
        if (resolved == null) {
            // 파이프라인이 굴리는 중이거나 이미 끝난 제품은 어드민이 할 일이 없다 — 상세로 보낸다.
            return "redirect:/admin/products/" + id;
        }
        model.addAttribute("workflowStep", resolved);
        model.addAttribute("workflowSteps", WORKFLOW_STEPS);
        model.addAttribute("workflowStepIndex", WORKFLOW_STEPS.indexOf(resolved));
        if ("ingredients".equals(resolved)) {
            model.addAttribute("productIngredients", service.getProductIngredients(id));
            model.addAttribute("propertyDefinitions", service.propertyDefinitions());
        }
        if ("functional".equals(resolved)) {
            // 한글 이름이 이미 있으면 화면을 여는 것만으로 자동 조회가 한 번 돈다. 없으면
            // 조회할 근거가 없으니 아무것도 하지 않고 입력 칸만 보여 준다.
            try {
                var screening = screeningService.findOrScreen(id).orElse(null);
                model.addAttribute("screening", screening);
                prefillFromScreening(request, product, screening);
            } catch (RuntimeException e) {
                // 외부 식약처 조회 실패가 검수 화면 전체를 500으로 만들지 않게 한다.
                model.addAttribute("screeningError", e.getMessage());
            }
        }
        return "product-workflow";
    }
}
