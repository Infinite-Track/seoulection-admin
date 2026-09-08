package com.seoulection.admin.product.infrastructure.repository;

import com.seoulection.admin.product.application.dto.ProductIngredientProperty;
import com.seoulection.admin.product.application.dto.ProductIngredientResult;
import com.seoulection.admin.product.application.dto.PropertyDefinitionResult;
import com.seoulection.admin.product.application.port.ProductIngredientPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * PostgreSQL 직접 접근 어댑터 — 지금의 기본 구현.
 *
 * <p>제품은 Mongo 에, 제품-성분 연결은 PostgreSQL 에 있다. {@code product_id} 는 Mongo 의 문자열 ID다.
 * 그래서 {@code product_ingredient.product_id} 에는 외래키를 걸 수 없다 —
 * 검수 대상 제품은 아직 {@code products_catalog} 에 없다.
 */
@Repository
@ConditionalOnProperty(name = "admin.product-ingredient.source", havingValue = "jdbc", matchIfMissing = true)
public class JdbcProductIngredientAdapter implements ProductIngredientPort {

    private final JdbcTemplate jdbc;

    public JdbcProductIngredientAdapter(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    @Transactional
    public void replace(String productId, List<String> rawNames, String source) {
        if (productId == null) return;
        jdbc.update("delete from product_ingredient where product_id = ?", productId);
        if (rawNames == null) return;
        for (int i = 0; i < rawNames.size(); i++) {
            String rawName = rawNames.get(i) == null ? "" : rawNames.get(i).trim();
            if (rawName.isBlank()) continue;
            jdbc.update("insert into product_ingredient(product_id, ingredient_id, raw_name, inci_order, source) values (?, ?, ?, ?, ?)",
                    productId, matchIngredient(rawName), rawName, i + 1, source == null ? "ADMIN" : source);
        }
    }

    /** 원문 → 성분 사전 id. 표기가 흔들리므로(WATER/Water/Aqua) 대소문자를 무시하고 별칭까지 본다. */
    private String matchIngredient(String rawName) {
        return jdbc.query("select i.id from ingredient i where lower(i.inci_name) = lower(?) "
                        + "union select ia.ingredient_id from ingredient_alias ia where lower(ia.alias) = lower(?) limit 1",
                ps -> { ps.setString(1, rawName); ps.setString(2, rawName); },
                rs -> rs.next() ? rs.getString(1) : null);
    }

    @Override
    public List<ProductIngredientResult> findByProductId(String productId) {
        // 특성은 행마다 개수가 달라 한 번에 모아 두고 붙인다(성분 30개에 쿼리 30번을 더 쏘지 않는다).
        Map<Long, List<ProductIngredientProperty>> properties = new LinkedHashMap<>();
        jdbc.query("""
                select p.product_ingredient_id, p.property_key, d.display_name_ko,
                       p.value_text, p.value_min, p.value_max, p.value_unit, p.notes
                from product_ingredient_property p
                join product_ingredient pi on pi.id = p.product_ingredient_id
                left join property_definition d on d.property_key = p.property_key
                where pi.product_id = ? order by p.property_key
                """, rs -> {
            properties.computeIfAbsent(rs.getLong("product_ingredient_id"), k -> new ArrayList<>())
                    .add(new ProductIngredientProperty(rs.getString("property_key"), rs.getString("display_name_ko"),
                            rs.getString("value_text"), rs.getBigDecimal("value_min"), rs.getBigDecimal("value_max"),
                            rs.getString("value_unit"), rs.getString("notes")));
        }, productId);

        return jdbc.query("""
                select pi.id, pi.ingredient_id, pi.raw_name, pi.inci_order,
                       pi.concentration_min, pi.concentration_max, pi.concentration_unit,
                       pi.notes, i.inci_name, i.display_name_ko
                from product_ingredient pi
                left join ingredient i on i.id = pi.ingredient_id
                where pi.product_id = ? order by pi.inci_order
                """, (rs, n) -> new ProductIngredientResult(rs.getLong("id"), rs.getString("ingredient_id"),
                rs.getString("raw_name"), rs.getInt("inci_order"), rs.getBigDecimal("concentration_min"),
                rs.getBigDecimal("concentration_max"), rs.getString("concentration_unit"),
                rs.getString("notes"), rs.getString("inci_name"), rs.getString("display_name_ko"),
                properties.getOrDefault(rs.getLong("id"), List.of())), productId);
    }

    @Override
    @Transactional
    public void review(String productId, long rowId, String ingredientId, BigDecimal min, BigDecimal max,
                       String unit, String notes, List<ProductIngredientProperty> properties) {
        int changed = jdbc.update("update product_ingredient set ingredient_id=?, concentration_min=?, concentration_max=?, concentration_unit=?, notes=? where id=? and product_id=?",
                blank(ingredientId), min, max, blank(unit), blank(notes), rowId, productId);
        if (changed == 0) throw new IllegalArgumentException("제품 성분 행을 찾을 수 없습니다.");

        if (properties == null) return;  // 미지정 = 특성은 건드리지 않는다
        jdbc.update("delete from product_ingredient_property where product_ingredient_id=?", rowId);
        for (ProductIngredientProperty property : properties) {
            String key = blank(property.propertyKey());
            if (key == null || isEmpty(property)) continue;   // 빈 칸은 저장하지 않는다 = 지운 것과 같다
            jdbc.update("""
                    insert into product_ingredient_property(product_ingredient_id, property_key, value_text,
                                                            value_min, value_max, value_unit, source, notes)
                    values (?,?,?,?,?,?, 'ADMIN', ?)
                    """, rowId, key, blank(property.valueText()), property.valueMin(), property.valueMax(),
                    blank(property.valueUnit()), blank(property.notes()));
        }
    }

    @Override
    public List<PropertyDefinitionResult> propertyDefinitions() {
        return jdbc.query("select property_key, display_name_ko, value_type, value_unit, description"
                        + " from property_definition order by display_name_ko",
                (rs, n) -> new PropertyDefinitionResult(rs.getString("property_key"), rs.getString("display_name_ko"),
                        rs.getString("value_type"), rs.getString("value_unit"), rs.getString("description")));
    }

    private boolean isEmpty(ProductIngredientProperty property) {
        return (property.valueText() == null || property.valueText().isBlank())
                && property.valueMin() == null && property.valueMax() == null;
    }

    private String blank(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
