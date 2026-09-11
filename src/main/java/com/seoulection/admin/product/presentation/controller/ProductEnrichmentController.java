package com.seoulection.admin.product.presentation.controller;

import com.seoulection.admin.product.infrastructure.repository.ProductEnrichmentRepository;
import com.seoulection.admin.product.presentation.dto.ProductEnrichmentRequest;
import com.seoulection.admin.product.domain.enums.ProductCategory;
import jakarta.validation.Validator;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.dao.DataAccessException;

@Controller
@RequestMapping("/admin/products/enrichment")
public class ProductEnrichmentController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ProductEnrichmentController.class);
    @org.springframework.beans.factory.annotation.Autowired
    private Validator validator;
    private final ProductEnrichmentRepository repository;
    private final com.seoulection.admin.product.infrastructure.ProductImageStorage images;
    public ProductEnrichmentController(ProductEnrichmentRepository repository, com.seoulection.admin.product.infrastructure.ProductImageStorage images) { this.repository = repository; this.images = images; }
    @GetMapping
    public String list(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "0") int page, Model model) {
        return "redirect:/admin/products?stage=product-info&status=NEED_PRODUCT_INFO" + (q.isBlank() ? "" : "&q=" + q);
    }
    @GetMapping("/{id}")
    public String edit(@PathVariable String id, Model model, RedirectAttributes flash) {
        try {
            model.addAttribute("product", repository.get(id));
            model.addAttribute("request", new ProductEnrichmentRequest());
            model.addAttribute("categories", ProductCategory.values());
            return "product-enrichment-form";
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/products?stage=product-info&status=NEED_PRODUCT_INFO";
        }
    }
    @PostMapping("/{id}")
    public String save(@PathVariable String id, @ModelAttribute("request") ProductEnrichmentRequest request,
                       BindingResult errors, @RequestParam(required = false) org.springframework.web.multipart.MultipartFile image, Model model, RedirectAttributes flash) {
        try {
            var product = repository.get(id);
            model.addAttribute("product", product);
            model.addAttribute("categories", ProductCategory.values());
            // 숨겨진 기존 필드는 클라이언트 입력이나 검증 오류를 신뢰하지 않는다.
            var filtered = new BeanPropertyBindingResult(request, "request");
            errors.getFieldErrors().stream().filter(e -> product.missing(e.getField())).forEach(filtered::addError);
            ProductEnrichmentRepository.FIELDS.forEach((field, key) -> {
                if (!product.missing(field)) return;
                if (field.equals("thumbnailUrl")) return;
                for (var violation : validator.validateProperty(request, field)) {
                    if (!filtered.hasFieldErrors(field)) filtered.rejectValue(field, "invalid", violation.getMessage());
                }
            });
            if (product.missing("productUrl") && !request.isUrlsValid())
                filtered.rejectValue("productUrl", "invalid", "올바른 http 또는 https URL을 입력해 주세요.");
            if (product.missing("thumbnailUrl")) {
                try { images.validate(image); }
                catch (IllegalArgumentException e) { filtered.reject("image", e.getMessage()); }
            }
            model.addAttribute(BindingResult.MODEL_KEY_PREFIX + "request", filtered);
            if (filtered.hasErrors()) return "product-enrichment-form";
            if (product.missing("thumbnailUrl")) {
                try { request.setThumbnailUrl(images.upload(image, product.asin())); }
                catch (RuntimeException e) {
                    log.warn("Product image upload failed productId={}", id, e);
                    model.addAttribute("errorMessage", "사진을 S3에 업로드하지 못했습니다. 사진을 다시 선택해 주세요.");
                    return "product-enrichment-form";
                }
            }
            repository.save(id, request);
            flash.addFlashAttribute("successMessage", "제품 정보를 저장했습니다. 다음 제품을 선택해 주세요.");
            return "redirect:/admin/products/enrichment";
        } catch (IllegalArgumentException | DataAccessException e) {
            log.warn("Product enrichment save failed productId={}", id, e);
            model.addAttribute("errorMessage", e instanceof IllegalArgumentException ? e.getMessage() : "저장에 실패했습니다. 입력 내용을 유지했으니 잠시 후 다시 시도해 주세요.");
            if (!model.containsAttribute("product")) model.addAttribute("product", new ProductEnrichmentRepository.Target(id, ""));
            model.addAttribute("categories", ProductCategory.values());
            return "product-enrichment-form";
        }
    }
}
