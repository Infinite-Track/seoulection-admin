package com.seoulection.admin.product.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

/**
 * 등록 폼과 검수 폼이 함께 쓰는 입력값.
 *
 * <p>기능성 "검수 여부"를 받는 필드는 없다 — 그건 status가 담고, 이 폼은 확인된 유형(function)만
 * 받는다. 유형이 비어 있으면 "확인했으나 기능성 아님"이다.
 */
public class ProductRegisterRequest {

    @NotBlank(message = "제품명을 입력해 주세요.")
    @Size(max = 200, message = "제품명은 200자 이하여야 합니다.")
    private String name;

    @NotBlank(message = "브랜드명을 입력해 주세요.")
    @Size(max = 100, message = "브랜드명은 100자 이하여야 합니다.")
    private String brand;
    private String nameKo;

    @NotBlank(message = "카테고리를 입력해 주세요.")
    @Pattern(
            regexp = "sunscreens|toners|treatments|moisturizers|cleansers|face masks",
            message = "제공된 카테고리 중 하나를 선택해 주세요."
    )
    private String category;

    /** 식약처 기능성 유형. REVIEWED_NONE 같은 별도 상태값 없이 이 목록의 유무가 결과를 말한다. */
    private List<String> function = new ArrayList<>();

    /**
     * 2단계 검수 화면의 라디오. NONE(기능성 아님) 또는 CONFIRMED(기능성 확인).
     *
     * <p>기본값을 두지 않는다. 미검수 제품에 "기능성 아님"이 미리 찍혀 있으면 확인 없이 저장만
     * 눌러도 식약처 기능성 아님이 사실로 기록된다 — 규제 정보라 기본값으로 정할 수 없다.
     */
    private String functionResult;

    private String ingredientResolution = "FOUND";
    private String ingredientsText;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getNameKo() { return nameKo; }
    public void setNameKo(String nameKo) { this.nameKo = nameKo; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public List<String> getFunction() { return function; }
    public void setFunction(List<String> value) { this.function = value == null ? new ArrayList<>() : value; }

    public String getFunctionResult() { return functionResult; }
    public void setFunctionResult(String value) { this.functionResult = value; }

    public String getIngredientResolution() { return ingredientResolution; }
    public void setIngredientResolution(String value) { this.ingredientResolution = value; }

    public String getIngredientsText() { return ingredientsText; }
    public void setIngredientsText(String value) { this.ingredientsText = value; }
}
