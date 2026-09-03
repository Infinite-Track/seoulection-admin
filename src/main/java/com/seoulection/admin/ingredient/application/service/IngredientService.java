package com.seoulection.admin.ingredient.application.service;

import com.seoulection.admin.ingredient.infrastructure.document.Ingredient;
import com.seoulection.admin.ingredient.infrastructure.repository.IngredientPostgresRepository;
import com.seoulection.admin.ingredient.infrastructure.document.Ingredient.EvidenceView;
import com.seoulection.admin.ingredient.infrastructure.document.Ingredient.EfficacyRangeView;
import com.seoulection.admin.ingredient.infrastructure.repository.PropertyDefinitionView;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Arrays;
import java.util.Map;
import com.seoulection.admin.ingredient.infrastructure.document.Ingredient.ConditionView;

@Service
public class IngredientService {
    private final IngredientPostgresRepository repository;

    public IngredientService(IngredientPostgresRepository repository) { this.repository = repository; }
    public List<Ingredient> getIngredients() { return repository.findAll(); }
    public List<PropertyDefinitionView> getPropertyDefinitions() { return repository.findPropertyDefinitions(); }

    public Ingredient getIngredient(String id) {
        Ingredient ingredient = repository.findById(id);
        if (ingredient == null) throw new IllegalArgumentException("성분을 찾을 수 없습니다: " + id);
        return ingredient;
    }

    public void create(String inciName, String displayNameKo, String family,
                       String aliasesText, String effectsText) {
        create(inciName, displayNameKo, family, aliasesText, effectsText, "", "", "", "");
    }

    public void create(String inciName, String displayNameKo, String family,
                       String aliasesText, String effectsText,
                       String propertiesText, String evidenceText, String efficacyRangesText) {
        create(inciName, displayNameKo, family, aliasesText, effectsText, propertiesText, evidenceText, efficacyRangesText, "");
    }

    public void create(String inciName, String displayNameKo, String family,
                       String aliasesText, String effectsText,
                       String propertiesText, String evidenceText, String efficacyRangesText, String efficacyConditionsText) {
        String id = java.util.UUID.randomUUID().toString();
        repository.save(id, inciName.trim(), displayNameKo.trim(), family.trim(),
                split(aliasesText), parseEffects(effectsText), parseMap(propertiesText));
        repository.replaceEvidence(id, parseEvidence(evidenceText));
        repository.replaceRanges(id, parseRanges(efficacyRangesText), parseConditions(efficacyConditionsText));
    }

    public void update(String id, String inciName, String displayNameKo, String family,
                       String aliasesText, String effectsText) {
        update(id, inciName, displayNameKo, family, aliasesText, effectsText, "", "", "", "");
    }

    public void update(String id, String inciName, String displayNameKo, String family,
                       String aliasesText, String effectsText,
                       String propertiesText, String evidenceText, String efficacyRangesText) {
        update(id, inciName, displayNameKo, family, aliasesText, effectsText, propertiesText, evidenceText, efficacyRangesText, "");
    }

    public void update(String id, String inciName, String displayNameKo, String family,
                       String aliasesText, String effectsText,
                       String propertiesText, String evidenceText, String efficacyRangesText, String efficacyConditionsText) {
        repository.save(id, inciName.trim(), displayNameKo.trim(), family.trim(),
                split(aliasesText), parseEffects(effectsText), parseMap(propertiesText));
        if (evidenceText != null && !evidenceText.isBlank()) repository.replaceEvidence(id, parseEvidence(evidenceText));
        if (efficacyRangesText != null && !efficacyRangesText.isBlank()) repository.replaceRanges(id, parseRanges(efficacyRangesText), parseConditions(efficacyConditionsText));
    }

    private List<String> split(String value) {
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.split("[,\\n]")).map(String::trim).filter(v -> !v.isBlank()).distinct().toList();
    }

    private Map<String, String> parseEffects(String value) {
        if (value == null || value.isBlank()) return Map.of();
        return Arrays.stream(value.split("[,\\n]"))
                .map(String::trim).filter(v -> v.contains("="))
                .<String[]>map(v -> v.split("=", 2))
                .collect(java.util.stream.Collectors.toMap(v -> v[0].trim(), v -> v[1].trim(), (a, b) -> b));
    }

    private Map<String, String> parseMap(String value) {
        return parseEffects(value);
    }

    private List<EvidenceView> parseEvidence(String value) {
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.split("\\n"))
                .map(String::trim).filter(v -> !v.isBlank()).map(v -> v.split("\\|", -1))
                .filter(v -> v.length >= 5 && !v[1].isBlank())
                .map(v -> new EvidenceView(null, v[0], v[1], blank(v, 2), Boolean.parseBoolean(blank(v, 3)),
                        blank(v, 4), blank(v, 5), Boolean.parseBoolean(blank(v, 6)), blank(v, 7), blank(v, 8), blank(v, 9), blank(v, 10)))
                .toList();
    }

    private List<EfficacyRangeView> parseRanges(String value) {
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.split("\\n"))
                .map(String::trim).filter(v -> !v.isBlank()).map(v -> v.split("\\|", -1))
                .filter(v -> v.length >= 1 && !v[0].isBlank())
                .map(v -> new EfficacyRangeView(v[0], blank(v, 1), blank(v, 2), blank(v, 3), blank(v, 4), blank(v, 5), blank(v, 6), longValue(v, 7), blank(v, 8)))
                .toList();
    }

    private List<ConditionView> parseConditions(String value) {
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.split("\\n"))
                .map(String::trim).filter(v -> !v.isBlank()).map(v -> v.split("\\|", -1))
                .filter(v -> v.length >= 2 && !v[0].isBlank() && !v[1].isBlank())
                .map(v -> new ConditionView(v[0].trim(), v[1].trim(), blank(v, 5), blank(v, 2), blank(v, 3), blank(v, 4),
                        blank(v, 6) == null ? "EXACT_VALUE" : blank(v, 6), Boolean.parseBoolean(blank(v, 7))))
                .toList();
    }

    private String blank(String[] values, int index) { return index < values.length && !values[index].isBlank() ? values[index].trim() : null; }
    private Long longValue(String[] values, int index) { return blank(values, index) == null ? null : Long.valueOf(values[index].trim()); }
}
