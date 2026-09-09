package com.seoulection.admin.product.infrastructure.repository;
import com.seoulection.admin.product.application.dto.PurchaseLinkResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestClient;
import java.util.Arrays; import java.util.List;
@Repository
public class PurchaseLinkRepository {
 private final RestClient client; private final String key;
 public PurchaseLinkRepository(@Value("${admin.product-service-url:http://localhost:8185}") String baseUrl, @Value("${APP_SERVICE_API_KEY:}") String key){this.client=RestClient.builder().baseUrl(baseUrl).build();this.key=key;}
 public List<PurchaseLinkResult> find(String productId){var r=client.get().uri("/internal/admin/v1/products/{id}/purchase-links",productId).header("X-Service-Key",key).retrieve().body(PurchaseLinkResponse[].class); return r==null?List.of():Arrays.stream(r).map(x->new PurchaseLinkResult(x.id(),x.url(),x.domain(),x.region(),x.active())).toList();}
 public void add(String productId,String url,String ignoredDomain,String region){client.post().uri("/internal/admin/v1/products/{id}/purchase-links",productId).header("X-Service-Key",key).body(new LinkRequest(url,region)).retrieve().toBodilessEntity();}
 public void delete(String productId,long id){client.patch().uri(uri->uri.path("/internal/admin/v1/products/{id}/purchase-links/{linkId}/active").queryParam("active",false).build(productId,id)).header("X-Service-Key",key).retrieve().toBodilessEntity();}
 public record LinkRequest(String url,String region){} public record PurchaseLinkResponse(long id,String url,String domain,String region,boolean active,String verifiedAt){}
}
