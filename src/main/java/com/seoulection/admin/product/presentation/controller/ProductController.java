package com.seoulection.admin.product.presentation.controller;

import com.seoulection.admin.product.application.service.ProductService;
import com.seoulection.admin.product.presentation.dto.ProductRegisterRequest;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping("/admin/products")
    public String page(Model model) {
        if (!model.containsAttribute("request")) {
            model.addAttribute("request", new ProductRegisterRequest());
        }
        model.addAttribute("products", service.getProducts());
        return "products";
    }

    @PostMapping("/admin/products")
    public String register(
            @Valid @ModelAttribute("request") ProductRegisterRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("products", service.getProducts());
            return "products";
        }

        service.register(request.getName(), request.getBrand(), request.getCategory());
        redirectAttributes.addFlashAttribute("successMessage", "제품을 등록했습니다.");
        return "redirect:/admin/products";
    }
}
