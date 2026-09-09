package com.seoulection.admin.product.infrastructure.repository;
import com.seoulection.admin.product.application.dto.PurchaseLinkResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public class PurchaseLinkRepository {
 private final JdbcTemplate jdbc;
 public PurchaseLinkRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}
 public List<PurchaseLinkResult> find(String productId){return jdbc.query("select id,url,domain,region,active from products_url where product_id=? order by active desc, created_at desc",(rs,n)->new PurchaseLinkResult(rs.getLong("id"),rs.getString("url"),rs.getString("domain"),rs.getString("region"),rs.getBoolean("active")),productId);}
 public void add(String productId,String url,String domain,String region){jdbc.update("insert into products_url(product_id,url,domain,region,active,created_at,updated_at) values (?,?,?,?,true,current_timestamp,current_timestamp)",productId,url,domain,region==null||region.isBlank()?"US":region);}
 public void delete(String productId,long id){jdbc.update("delete from products_url where id=? and product_id=?",id,productId);}
}
