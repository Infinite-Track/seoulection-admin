package com.seoulection.admin.product.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ProductRegisterRequest {

    @NotBlank(message = "제품명을 입력해 주세요.")
    @Size(max = 200, message = "제품명은 200자 이하여야 합니다.")
    private String name;

    @NotBlank(message = "브랜드명을 입력해 주세요.")
    @Size(max = 100, message = "브랜드명은 100자 이하여야 합니다.")
    private String brand;

    @NotBlank(message = "카테고리를 입력해 주세요.")
    @Pattern(
            regexp = "sunscreens|toners|treatments|moisturizers|cleansers|face masks",
            message = "제공된 카테고리 중 하나를 선택해 주세요."
    )
    private String category;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
