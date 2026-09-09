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
 /**
  * 이 제품이 사용자 카탈로그(products_catalog)에 있는가.
  *
  * <p>구매 링크는 그 테이블을 참조하는 FK 라, 없는 제품에 넣으면 DB 가 거부한다.
  * 지금 파이프라인은 아마존 링크가 있는 제품만 분석 끝에 카탈로그로 올리므로,
  * 그 전 단계(검수 중) 제품에는 링크를 달 수 없다 — 화면이 그 사실을 먼저 알려 준다.
  *
  * <p>나중에 "아마존이 없어도 다른 판매처 링크가 있으면 카탈로그 큐에 넣는다"로 바뀌면
  * 이 판정은 그대로 두고 파이프라인 쪽만 바뀐다(여기는 여전히 "카탈로그에 있는가"다).
  */
 public boolean registeredInCatalog(String productId){
  Long count = jdbc.queryForObject("select count(*) from products_catalog where id=?", Long.class, productId);
  return count != null && count > 0;
 }

 /**
  * 노출/숨김 전환.
  *
  * <p>지우지 않고 숨기는 쪽을 둔 이유: 판매처가 일시 품절이거나 링크가 잠깐 죽었을 때
  * 지웠다가 다시 넣으면 어떤 링크였는지 기록이 사라진다. 숨겨 두면 되살리기만 하면 된다.
  */
 public void setActive(String productId,long id,boolean active){
  jdbc.update("update products_url set active=?, updated_at=current_timestamp where id=? and product_id=?",active,id,productId);
 }

 public void delete(String productId,long id){jdbc.update("delete from products_url where id=? and product_id=?",id,productId);}
}
