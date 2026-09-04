package com.seoulection.admin.product.infrastructure.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 제품은 Mongo에, 제품-성분 연결은 PostgreSQL에 둔다. product_id는 Mongo의 문자열 ID다. */
@Repository
public class ProductIngredientPostgresRepository {
    private final JdbcTemplate jdbc;

    public ProductIngredientPostgresRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Transactional
    public void replace(String productId, List<String> rawNames, String source) {
        if (productId == null) return;
        jdbc.update("delete from product_ingredient where product_id = ?", productId);
        if (rawNames == null) return;
        for (int i = 0; i < rawNames.size(); i++) {
            String rawName = rawNames.get(i) == null ? "" : rawNames.get(i).trim();
            if (rawName.isBlank()) continue;
            String ingredientId = jdbc.query("select i.id from ingredient i where lower(i.inci_name) = lower(?) " +
                            "union select ia.ingredient_id from ingredient_alias ia where lower(ia.alias) = lower(?) limit 1",
                    ps -> { ps.setString(1, rawName); ps.setString(2, rawName); },
                    rs -> rs.next() ? rs.getString(1) : null);
            jdbc.update("insert into product_ingredient(product_id, ingredient_id, raw_name, inci_order, source) values (?, ?, ?, ?, ?)",
                    productId, ingredientId, rawName, i + 1, source == null ? "ADMIN" : source);
        }
    }
}
