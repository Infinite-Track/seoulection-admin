package com.seoulection.admin.ingredient.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class IngredientCreateRequest {
    @NotBlank(message = "표준 성분명을 입력해 주세요.") @Size(max = 200)
    @NotBlank(message = "INCI명을 입력해 주세요.") @Size(max = 200)
    private String inciName;
    @NotBlank(message = "한글명을 입력해 주세요.") @Size(max = 100)
    private String displayNameKo;
    @NotBlank(message = "성분 계열을 입력해 주세요.") @Size(max = 80)
    private String family;
    private String aliasesText;
    private String effectsText;
    private String propertiesText;
    private String evidenceText;
    private String efficacyRangesText;
    private String efficacyConditionsText;

    public String getInciName() { return inciName; }
    public void setInciName(String value) { inciName = value; }
    public String getDisplayNameKo() { return displayNameKo; }
    public void setDisplayNameKo(String value) { displayNameKo = value; }
    public String getFamily() { return family; }
    public void setFamily(String value) { family = value; }
    public String getAliasesText() { return aliasesText; }
    public void setAliasesText(String value) { aliasesText = value; }
    public String getEffectsText() { return effectsText; }
    public void setEffectsText(String value) { effectsText = value; }
    public String getPropertiesText() { return propertiesText; }
    public void setPropertiesText(String value) { propertiesText = value; }
    public String getEvidenceText() { return evidenceText; }
    public void setEvidenceText(String value) { evidenceText = value; }
    public String getEfficacyRangesText() { return efficacyRangesText; }
    public void setEfficacyRangesText(String value) { efficacyRangesText = value; }
    public String getEfficacyConditionsText() { return efficacyConditionsText; }
    public void setEfficacyConditionsText(String value) { efficacyConditionsText = value; }
}
