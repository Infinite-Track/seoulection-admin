package com.seoulection.admin.ingredient.application.service;

import com.seoulection.admin.ingredient.infrastructure.document.IngredientDocument;
import com.seoulection.admin.ingredient.infrastructure.repository.IngredientMongoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Arrays;
import java.util.UUID;
import java.util.Map;

@Service
public class IngredientService {
    private final IngredientMongoRepository repository;

    public IngredientService(IngredientMongoRepository repository) { this.repository = repository; }
    public List<IngredientDocument> getIngredients() { return repository.findAll(); }

    public IngredientDocument getIngredient(String id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("성분을 찾을 수 없습니다: " + id));
    }

    public void create(String canonicalName, String inciName, String displayNameKo, String family,
                       String aliasesText, String searchGroupsText, String effectsText) {
        repository.save(new IngredientDocument(UUID.randomUUID().toString(), canonicalName.trim(), inciName.trim(),
                displayNameKo.trim(), family.trim(), split(aliasesText), split(searchGroupsText), parseEffects(effectsText), Map.of()));
    }

    public void update(String id, String canonicalName, String inciName, String displayNameKo, String family,
                       String aliasesText, String searchGroupsText, String effectsText) {
        repository.save(new IngredientDocument(id, canonicalName.trim(), inciName.trim(), displayNameKo.trim(), family.trim(),
                split(aliasesText), split(searchGroupsText), parseEffects(effectsText), Map.of()));
    }

    private List<String> split(String value) {
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.split("[,\\n]")).map(String::trim).filter(v -> !v.isBlank()).distinct().toList();
    }

    private Map<String, String> parseEffects(String value) {
        if (value == null || value.isBlank()) return Map.of();
        return Arrays.stream(value.split("[,\\n]"))
                .map(String::trim).filter(v -> v.contains("="))
                .map(v -> v.split("=", 2))
                .collect(java.util.stream.Collectors.toMap(v -> v[0].trim(), v -> v[1].trim(), (a, b) -> b));
    }
}
