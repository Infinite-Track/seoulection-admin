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

    /**
     * 전성분 목록을 이 제품의 성분 행에 반영한다.
     *
     * <p>⚠️ 전부 지우고 다시 넣지 않는다. 그렇게 하면 <b>전성분을 다시 저장하는 순간 함량과
     * 특성이 통째로 사라진다</b>(특성은 product_ingredient 를 ON DELETE CASCADE 로 따라간다).
     * 오타 하나 고치려고 전성분을 다시 저장했다가 몇 시간치 보완 입력이 날아가는 일이라,
     * 살아남는 성분은 <b>행을 그대로 두고</b> 순서만 고친다.
     *
     * <p>순서를 한 번 음수로 미는 이유: {@code unique (product_id, inci_order, raw_name)} 때문에
     * 성분 순서가 바뀌면 중간에 같은 순번이 겹치는 순간이 생긴다. 음수로 피신시켰다가 확정한다.
     */
    @Override
    @Transactional
    public void replace(String productId, List<String> rawNames, String source) {
        if (productId == null) return;
        String origin = source == null ? "ADMIN" : source;
        List<String> names = rawNames == null ? List.of() : rawNames.stream()
                .filter(java.util.Objects::nonNull).map(String::trim)
                .filter(name -> !name.isBlank()).distinct().toList();

        if (names.isEmpty()) {
            jdbc.update("delete from product_ingredient where product_id = ?", productId);
            return;
        }

        // 1) 목록에서 빠진 성분만 지운다. 남은 성분의 함량·특성은 건드리지 않는다.
        String placeholders = String.join(",", java.util.Collections.nCopies(names.size(), "?"));
        Object[] deleteArgs = java.util.stream.Stream.concat(
                java.util.stream.Stream.of((Object) productId), names.stream()).toArray();
        jdbc.update("delete from product_ingredient where product_id = ? and raw_name not in ("
                + placeholders + ")", deleteArgs);

        // 2) 순번 충돌 회피.
        jdbc.update("update product_ingredient set inci_order = -inci_order "
                + "where product_id = ? and inci_order > 0", productId);

        // 3) 살아남은 행은 순서만, 새 성분은 삽입.
        for (int i = 0; i < names.size(); i++) {
            String rawName = names.get(i);
            String matched = matchIngredient(rawName);
            int updated = jdbc.update("update product_ingredient set inci_order = ?, source = ?, "
                            // 사람이 손으로 연결해 둔 값은 덮지 않는다. 비어 있을 때만 채운다 —
                            // 사전에 성분이 늦게 등록돼도 다음 저장에서 자동으로 이어진다.
                            + "ingredient_id = coalesce(ingredient_id, ?) "
                            + "where product_id = ? and raw_name = ?",
                    i + 1, origin, matched, productId, rawName);
            if (updated == 0) {
                jdbc.update("insert into product_ingredient(product_id, ingredient_id, raw_name, inci_order, source)"
                        + " values (?, ?, ?, ?, ?)", productId, matched, rawName, i + 1, origin);
            }
        }
    }

    /**
     * 원문 → 성분 사전 id.
     *
     * <p>한글명({@code display_name_ko})까지 보는 이유: 국내 제품의 전성분은 한글로 적힌다
     * ("나이아신아마이드"). INCI 명과 별칭만 보면 <b>한글 전성분은 한 건도 연결되지 않아</b>
     * 성분별 보완 화면이 늘 비게 된다.
     *
     * <p>공백을 지우고 비교한다 — "히알루론산 나트륨"과 "히알루론산나트륨"은 같은 성분이다.
     */
    private String matchIngredient(String rawName) {
        return jdbc.query("""
                        select i.id from ingredient i
                         where lower(replace(i.inci_name, ' ', '')) = lower(replace(?, ' ', ''))
                            or lower(replace(i.display_name_ko, ' ', '')) = lower(replace(?, ' ', ''))
                        union
                        select ia.ingredient_id from ingredient_alias ia
                         where lower(replace(ia.alias, ' ', '')) = lower(replace(?, ' ', ''))
                        limit 1
                        """,
                ps -> { ps.setString(1, rawName); ps.setString(2, rawName); ps.setString(3, rawName); },
                rs -> rs.next() ? rs.getString(1) : null);
    }

    @Override
    public List<ProductIngredientResult> findByProductId(String productId) {
        // 특성은 행마다 개수가 달라 한 번에 모아 두고 붙인다(성분 30개에 쿼리 30번을 더 쏘지 않는다).
        Map<Long, List<ProductIngredientProperty>> properties = new LinkedHashMap<>();
        jdbc.query("""
                select p.product_ingredient_id, p.property_key, d.display_name_ko, d.value_type,
                       p.value_text, p.value_min, p.value_max, p.value_unit, p.notes
                from product_ingredient_property p
                join product_ingredient pi on pi.id = p.product_ingredient_id
                left join property_definition d on d.property_key = p.property_key
                where pi.product_id = ? order by p.property_key
                """, rs -> {
            properties.computeIfAbsent(rs.getLong("product_ingredient_id"), k -> new ArrayList<>())
                    .add(new ProductIngredientProperty(rs.getString("property_key"), rs.getString("display_name_ko"),
                            rs.getString("value_type"), rs.getString("value_text"),
                            rs.getBigDecimal("value_min"), rs.getBigDecimal("value_max"),
                            rs.getString("value_unit"), rs.getString("notes")));
        }, productId);

        return jdbc.query("""
                select pi.id, pi.ingredient_id, pi.raw_name, pi.inci_order,
                       pi.concentration_min, pi.concentration_max, pi.concentration_unit,
                       null::text as notes, i.inci_name, i.display_name_ko
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
        // ⚠️ ingredient_id 는 건드리지 않는다. 사전 연결은 전성분 저장 때 matchIngredient 가
        //    정하는 것이고, 화면에는 손으로 고칠 칸이 없다(연결된 성분만 이 폼이 열린다).
        //    예전처럼 폼 값으로 덮으면, 값이 비어 온 순간 연결이 끊기고 그 행은 편집 목록에서
        //    사라져 버린다 — 방금 저장한 성분이 화면에서 없어지는 셈이다.
        int changed = jdbc.update("update product_ingredient set concentration_min=?, concentration_max=?,"
                        + " concentration_unit=? where id=? and product_id=?",
                min, max, blank(unit), rowId, productId);
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
