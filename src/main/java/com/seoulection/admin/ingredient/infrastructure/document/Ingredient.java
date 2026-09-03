package com.seoulection.admin.ingredient.infrastructure.document;

import java.util.List;
import java.util.Map;

public class Ingredient {
    private String id;
    private String inciName;
    private String displayNameKo;
    private String family;
    private List<String> aliases;
    private Map<String, String> effects;
    private Map<String, String> properties;
    private List<EvidenceView> evidences;
    private List<EfficacyRangeView> efficacyRanges;

    protected Ingredient() { }

    public Ingredient(String id, String inciName, String displayNameKo,
                              String family, List<String> aliases,
                              Map<String, String> effects, Map<String, String> properties) {
        this(id, inciName, displayNameKo, family, aliases, effects, properties, List.of(), List.of());
    }

    public Ingredient(String id, String inciName, String displayNameKo,
                              String family, List<String> aliases,
                              Map<String, String> effects, Map<String, String> properties,
                              List<EvidenceView> evidences, List<EfficacyRangeView> efficacyRanges) {
        this.id = id; this.inciName = inciName; this.displayNameKo = displayNameKo; this.family = family; this.aliases = aliases;
        this.effects = effects; this.properties = properties;
        this.evidences = evidences; this.efficacyRanges = efficacyRanges;
    }

    public String getId() { return id; }
    public String getInciName() { return inciName; }
    public String getDisplayNameKo() { return displayNameKo; }
    public String getFamily() { return family; }
    public List<String> getAliases() { return aliases == null ? List.of() : aliases; }
    public Map<String, String> getEffects() { return effects == null ? Map.of() : effects; }
    public Map<String, String> getProperties() { return properties == null ? Map.of() : properties; }
    public List<EvidenceView> getEvidences() { return evidences == null ? List.of() : evidences; }
    public List<EfficacyRangeView> getEfficacyRanges() { return efficacyRanges == null ? List.of() : efficacyRanges; }

    public record EvidenceView(Long id, String sourceType, String title, String url, boolean humanEvidence,
                               String evidenceLevel, String targetScore, boolean ingredientSpecific,
                               String productForm, String studyConcentration, String studyConcentrationUnit,
                               String summary) { }

    public record EfficacyRangeView(String targetKey, String productType, String concentrationMin,
                                    String concentrationMax, String concentrationUnit,
                                    String onsetConcentration, String irritationConcentration,
                                    Long evidenceId, String notes, String role, List<ConditionView> conditions) {
        public EfficacyRangeView(String targetKey, String productType, String concentrationMin,
                                 String concentrationMax, String concentrationUnit,
                                 String onsetConcentration, String irritationConcentration,
                                 Long evidenceId, String notes) {
            this(targetKey, productType, concentrationMin, concentrationMax, concentrationUnit,
                    onsetConcentration, irritationConcentration, evidenceId, notes, "SUPPORT", List.of());
        }
    }

    public record ConditionView(String targetKey, String parameterKey, String valueText,
                                String valueMin, String valueMax, String valueUnit,
                                String conditionMode, boolean interpolationAllowed) { }
}
