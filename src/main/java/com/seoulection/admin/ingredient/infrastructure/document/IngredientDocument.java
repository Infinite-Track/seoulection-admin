package com.seoulection.admin.ingredient.infrastructure.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;
import java.util.Map;

@Document(collection = "ingredients")
public class IngredientDocument {
    @Id private String id;
    private String canonicalName;
    private String inciName;
    private String displayNameKo;
    private String family;
    private List<String> aliases;
    private List<String> searchGroups;
    private Map<String, String> effects;
    private Map<String, String> properties;

    protected IngredientDocument() { }

    public IngredientDocument(String id, String canonicalName, String inciName, String displayNameKo,
                              String family, List<String> aliases, List<String> searchGroups,
                              Map<String, String> effects, Map<String, String> properties) {
        this.id = id; this.canonicalName = canonicalName; this.inciName = inciName;
        this.displayNameKo = displayNameKo; this.family = family; this.aliases = aliases;
        this.searchGroups = searchGroups; this.effects = effects; this.properties = properties;
    }

    public String getId() { return id; }
    public String getCanonicalName() { return canonicalName; }
    public String getInciName() { return inciName; }
    public String getDisplayNameKo() { return displayNameKo; }
    public String getFamily() { return family; }
    public List<String> getAliases() { return aliases == null ? List.of() : aliases; }
    public List<String> getSearchGroups() { return searchGroups == null ? List.of() : searchGroups; }
    public Map<String, String> getEffects() { return effects == null ? Map.of() : effects; }
    public Map<String, String> getProperties() { return properties == null ? Map.of() : properties; }
}
