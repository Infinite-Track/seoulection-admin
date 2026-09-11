package com.seoulection.admin.product.presentation.dto;

import jakarta.validation.constraints.*;
import java.net.URI;

public class ProductEnrichmentRequest {
    @NotBlank(message = "제품명을(를) 입력해 주세요.")
    @Size(max = 200)
    private String name;
    public String getName() { return name; }
    public void setName(String value) { this.name = value == null ? null : value.trim(); }

    @Size(max = 200)
    private String nameKo;
    public String getNameKo() { return nameKo; }
    public void setNameKo(String value) { this.nameKo = value == null || value.isBlank() ? null : value.trim(); }

    @NotBlank(message = "브랜드을(를) 입력해 주세요.")
    @Size(max = 100)
    private String brand;
    public String getBrand() { return brand; }
    public void setBrand(String value) { this.brand = value == null ? null : value.trim(); }

    @NotBlank(message = "카테고리을(를) 입력해 주세요.")
    @Size(max = 40)
    @Pattern(regexp = "sunscreens|toners|treatments|moisturizers|cleansers|face masks", message = "카테고리를 선택해 주세요.")
    private String category;
    public String getCategory() { return category; }
    public void setCategory(String value) { this.category = value == null ? null : value.trim(); }

    @NotNull(message = "가격을 입력해 주세요.")
    @DecimalMin(value = "0", message = "가격은 0 이상이어야 합니다.")
    @Digits(integer = 10, fraction = 2, message = "가격은 정수 10자리, 소수점 2자리까지 입력할 수 있습니다.")
    private java.math.BigDecimal price;
    public java.math.BigDecimal getPrice() { return price; }
    public void setPrice(java.math.BigDecimal price) { this.price = price; }

    @Size(max = 2048)
    private String thumbnailUrl;
    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String value) { this.thumbnailUrl = value == null ? null : value.trim(); }

    @Size(max = 10000)
    private String description;
    public String getDescription() { return description; }
    public void setDescription(String value) { this.description = value == null ? null : value.trim(); }

    @NotBlank(message = "제휴링크을(를) 입력해 주세요.")
    @Size(max = 2048)
    private String productUrl;
    public String getProductUrl() { return productUrl; }
    public void setProductUrl(String value) { this.productUrl = value == null ? null : value.trim(); }

    @AssertTrue(message = "사진과 제휴링크는 올바른 http 또는 https URL이어야 합니다.")
    public boolean isUrlsValid() { return validUrl(productUrl); }
    private boolean validUrl(String value) {
        if (value == null || value.isBlank()) return true;
        try {
            URI uri = URI.create(value);
            return ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null && uri.getUserInfo() == null;
        } catch (IllegalArgumentException e) { return false; }
    }
}
