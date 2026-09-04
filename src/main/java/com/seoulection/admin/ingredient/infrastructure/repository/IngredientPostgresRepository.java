package com.seoulection.admin.ingredient.infrastructure.repository;

import com.seoulection.admin.ingredient.infrastructure.document.Ingredient;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import com.seoulection.admin.ingredient.infrastructure.document.Ingredient.EvidenceView;
import com.seoulection.admin.ingredient.infrastructure.document.Ingredient.EfficacyRangeView;
import com.seoulection.admin.ingredient.infrastructure.document.Ingredient.ConditionView;

@Repository
public class IngredientPostgresRepository {
    private final JdbcTemplate jdbc;

    public IngredientPostgresRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<Ingredient> findAll() {
        return jdbc.query("select id, inci_name, display_name_ko, family from ingredient order by id",
                (rs, n) -> toIngredient(rs.getString("id"), rs.getString("inci_name"), rs.getString("display_name_ko"), rs.getString("family")));
    }

    public Ingredient findById(String id) {
        return jdbc.query("select id, inci_name, display_name_ko, family from ingredient where id = ?", ps -> ps.setString(1, id),
                rs -> rs.next() ? toIngredient(rs.getString("id"), rs.getString("inci_name"), rs.getString("display_name_ko"), rs.getString("family")) : null);
    }

    public List<PropertyDefinitionView> findPropertyDefinitions() {
        return jdbc.query("select property_key, display_name_ko, value_type, value_unit, description from property_definition order by property_key",
                (rs, n) -> new PropertyDefinitionView(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)));
    }

    public void savePropertyDefinition(String key, String displayNameKo, String valueType, String valueUnit, String description) {
        jdbc.update("insert into property_definition(property_key, display_name_ko, value_type, value_unit, description) values (?, ?, ?, ?, ?) " +
                        "on conflict (property_key) do update set display_name_ko=excluded.display_name_ko, value_type=excluded.value_type, value_unit=excluded.value_unit, description=excluded.description",
                key.trim().toUpperCase(), displayNameKo.trim(), valueType.trim().toUpperCase(), blank(valueUnit), blank(description));
    }

    public void deletePropertyDefinition(String key) {
        if (jdbc.queryForObject("select count(*) from ingredient_property where property_key = ?", Long.class, key) > 0
                || jdbc.queryForObject("select count(*) from product_ingredient_property where property_key = ?", Long.class, key) > 0) {
            throw new IllegalArgumentException("사용 중인 특성은 삭제할 수 없습니다. 값을 먼저 정리해 주세요.");
        }
        jdbc.update("delete from property_definition where property_key = ?", key);
    }

    @Transactional
    public void save(String id, String inci, String ko, String family,
                     List<String> aliases, Map<String, String> effects,
                     Map<String, String> properties) {
        String actualId = id == null || id.isBlank() ? UUID.randomUUID().toString() : id;
        jdbc.update("insert into ingredient (id, inci_name, display_name_ko, family) values (?, ?, ?, ?) " +
                        "on conflict (id) do update set inci_name=excluded.inci_name, display_name_ko=excluded.display_name_ko, family=excluded.family",
                actualId, inci, ko, family);
        jdbc.update("delete from ingredient_alias where ingredient_id = ?", actualId);
        aliases.forEach(alias -> jdbc.update("insert into ingredient_alias (ingredient_id, alias, alias_type) values (?, ?, 'SYNONYM') on conflict do nothing", actualId, alias));
        jdbc.update("delete from ingredient_effect where ingredient_id = ?", actualId);
        effects.forEach((target, role) -> jdbc.update("insert into ingredient_effect (ingredient_id, target_key, role) values (?, ?, ?) on conflict (ingredient_id, target_key) do update set role=excluded.role", actualId, target, role));
        // 현재 관리자 폼은 특성을 편집하지 않으므로 수정 시 기존 특성을 보존한다.
        if (!properties.isEmpty()) {
            properties.keySet().forEach(key -> {
                if (jdbc.queryForObject("select count(*) from property_definition where property_key = ?", Long.class, key) == 0) {
                    throw new IllegalArgumentException("등록되지 않은 특성 키입니다: " + key);
                }
            });
            jdbc.update("delete from ingredient_property where ingredient_id = ?", actualId);
            properties.forEach((key, value) -> jdbc.update("insert into ingredient_property (ingredient_id, property_key, value_text) values (?, ?, ?) on conflict (ingredient_id, property_key) do update set value_text=excluded.value_text", actualId, key, value));
        }
    }

    @Transactional
    public void replaceEvidence(String ingredientId, List<EvidenceView> evidences) {
        jdbc.update("delete from ingredient_evidence where ingredient_id = ?", ingredientId);
        for (EvidenceView evidence : evidences) {
            Long sourceId = jdbc.queryForObject("insert into evidence_source(source_type, title, url) values (?, ?, ?) returning id",
                    Long.class, evidence.sourceType(), evidence.title(), evidence.url());
            Long evidenceId = jdbc.queryForObject("insert into ingredient_evidence(ingredient_id, source_id, human_evidence, evidence_level, target_score, ingredient_specific, summary) values (?, ?, ?, ?, ?, ?, ?) returning id",
                    Long.class,
                    ingredientId, sourceId, evidence.humanEvidence(), evidence.evidenceLevel(), evidence.targetScore(), evidence.ingredientSpecific(),
                    evidence.summary());
            if (evidence.productForm() != null && !evidence.productForm().isBlank()) {
                jdbc.update("insert into ingredient_evidence_parameter(evidence_id, parameter_key, value_text) values (?, 'PRODUCT_FORM', ?)", evidenceId, evidence.productForm());
            }
            if (evidence.studyConcentration() != null && !evidence.studyConcentration().isBlank()) {
                jdbc.update("insert into ingredient_evidence_parameter(evidence_id, parameter_key, value_min, value_max, value_unit) values (?, 'STUDY_CONCENTRATION', ?, ?, ?)",
                        evidenceId, decimal(evidence.studyConcentration()), decimal(evidence.studyConcentration()), evidence.studyConcentrationUnit());
            }
        }
    }

    @Transactional
    public void replaceRanges(String ingredientId, List<EfficacyRangeView> ranges, List<ConditionView> conditions) {
        jdbc.update("delete from ingredient_efficacy_profile where ingredient_id = ?", ingredientId);
        for (EfficacyRangeView range : ranges) {
            Long rangeId = jdbc.queryForObject("insert into ingredient_efficacy_profile(ingredient_id, target_key, product_type, concentration_min, concentration_max, concentration_unit, onset_concentration, irritation_concentration, notes) values (?, ?, ?, ?, ?, ?, ?, ?, ?) returning id",
                    Long.class,
                    ingredientId, range.targetKey(), range.productType(), decimal(range.concentrationMin()), decimal(range.concentrationMax()), range.concentrationUnit(),
                    decimal(range.onsetConcentration()), decimal(range.irritationConcentration()), range.notes());
            if (range.evidenceId() != null) {
                jdbc.update("insert into ingredient_efficacy_profile_evidence(profile_id, evidence_id) values (?, ?)", rangeId, range.evidenceId());
            }
        }
        for (ConditionView condition : conditions) {
            Long rangeId = jdbc.queryForObject("select id from ingredient_efficacy_profile where ingredient_id = ? and target_key = ? order by id limit 1", Long.class, ingredientId, condition.targetKey());
            if (rangeId != null) {
                jdbc.update("insert into ingredient_efficacy_profile_condition(profile_id, parameter_key, value_text, value_min, value_max, value_unit, condition_mode, interpolation_allowed) values (?, ?, ?, ?, ?, ?, ?, ?) on conflict (profile_id, parameter_key) do update set value_text=excluded.value_text, value_min=excluded.value_min, value_max=excluded.value_max, value_unit=excluded.value_unit, condition_mode=excluded.condition_mode, interpolation_allowed=excluded.interpolation_allowed",
                        rangeId, condition.parameterKey(), condition.valueText(), decimal(condition.valueMin()), decimal(condition.valueMax()), condition.valueUnit(), condition.conditionMode(), condition.interpolationAllowed());
            }
        }
    }

    private BigDecimal decimal(String value) {
        return value == null || value.isBlank() ? null : new BigDecimal(value.trim());
    }

    private Ingredient toIngredient(String id, String inci, String ko, String family) {
        List<String> aliases = jdbc.query("select alias from ingredient_alias where ingredient_id = ? order by id", ps -> ps.setString(1, id),
                (rs, n) -> rs.getString("alias"));
        Map<String, String> effects = jdbc.query("select target_key, role from ingredient_effect where ingredient_id = ? order by id", ps -> ps.setString(1, id),
                rs -> {
                    Map<String, String> result = new java.util.LinkedHashMap<>();
                    while (rs.next()) result.put(rs.getString("target_key"), rs.getString("role"));
                    return result;
                });
        Map<String, String> properties = jdbc.query("select property_key, coalesce(value_text, value_min::text, value_max::text) from ingredient_property where ingredient_id = ? order by id", ps -> ps.setString(1, id),
                rs -> {
                    Map<String, String> result = new java.util.LinkedHashMap<>();
                    while (rs.next()) result.put(rs.getString(1), rs.getString(2));
                    return result;
                });
        return new Ingredient(id, inci, ko, family,
                aliases, effects, properties, findEvidence(id), findRanges(id));
    }

    private List<String> split(String value) {
        if (value == null || value.isBlank()) return List.of();
        return java.util.Arrays.stream(value.split(",")).map(String::trim).filter(v -> !v.isBlank()).toList();
    }

    private String blank(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    private List<EvidenceView> findEvidence(String ingredientId) {
        return jdbc.query("select ie.id, es.source_type, es.title, es.url, ie.human_evidence, ie.evidence_level, ie.target_score, ie.ingredient_specific, " +
                        "(select value_text from ingredient_evidence_parameter p where p.evidence_id = ie.id and p.parameter_key = 'PRODUCT_FORM'), " +
                        "(select coalesce(value_min::text, value_max::text, value_text) from ingredient_evidence_parameter p where p.evidence_id = ie.id and p.parameter_key = 'STUDY_CONCENTRATION'), " +
                        "(select value_unit from ingredient_evidence_parameter p where p.evidence_id = ie.id and p.parameter_key = 'STUDY_CONCENTRATION'), ie.summary " +
                        "from ingredient_evidence ie join evidence_source es on es.id = ie.source_id where ie.ingredient_id = ? order by ie.id", ps -> ps.setString(1, ingredientId),
                (rs, n) -> new EvidenceView(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getBoolean(5),
                        rs.getString(6), rs.getString(7), rs.getBoolean(8), rs.getString(9),
                        rs.getString(10), rs.getString(11), rs.getString(12)));
    }

    private List<EfficacyRangeView> findRanges(String ingredientId) {
        return jdbc.query("select p.id, p.target_key, p.product_type, p.concentration_min, p.concentration_max, p.concentration_unit, p.onset_concentration, p.irritation_concentration, p.role, " +
                        "(select e.evidence_id from ingredient_efficacy_profile_evidence e where e.profile_id = p.id order by e.evidence_id limit 1), p.notes " +
                        "from ingredient_efficacy_profile p where p.ingredient_id = ? order by p.id", ps -> ps.setString(1, ingredientId),
                (rs, n) -> new EfficacyRangeView(rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5),
                        rs.getString(6), rs.getString(7), rs.getString(8), (Long) rs.getObject(10), rs.getString(11), rs.getString(9), findConditions(rs.getLong(1), rs.getString(2))));
    }

    private List<ConditionView> findConditions(Long rangeId, String targetKey) {
        return jdbc.query("select parameter_key, value_text, value_min, value_max, value_unit, condition_mode, interpolation_allowed from ingredient_efficacy_profile_condition where profile_id = ? order by id",
                ps -> ps.setLong(1, rangeId), (rs, n) -> new ConditionView(targetKey, rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getBoolean(7)));
    }
}
