package com.seoulection.admin.product.infrastructure.repository;

import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoClient;
import com.seoulection.admin.product.presentation.dto.ProductEnrichmentRequest;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

@Testcontainers
class ProductEnrichmentRepositoryTest {
    @Container static MongoDBContainer db = new MongoDBContainer("mongo:8");
    MongoClient client;
    MongoTemplate mongo;
    ProductEnrichmentRepository repository;
    @BeforeEach void setup() {
        client = MongoClients.create(db.getReplicaSetUrl());
        mongo = new MongoTemplate(client,"enrichment_test");
        mongo.dropCollection("products");
        repository = new ProductEnrichmentRepository(mongo);
    }
    @AfterEach void close() { client.close(); }
    @Test void selectsOnlyNeedProductInfoRegardlessOfExistingFields() {
        var rows = mongo.getCollection("products");
        rows.insertOne(new Document("_id","bare").append("asin","BARE").append("status","NEED_PRODUCT_INFO"));
        rows.insertOne(new Document("_id","partial").append("asin","PARTIAL")
                .append("status","NEED_PRODUCT_INFO").append("name","Existing name").append("price",10));
        rows.insertOne(new Document("_id","pending").append("asin","PENDING").append("status","PENDING"));
        rows.insertOne(new Document("_id","no-status").append("asin","UNKNOWN"));
        assertThat(repository.find("",0).getContent()).extracting(ProductEnrichmentRepository.Target::id)
                .containsExactlyInAnyOrder("bare","partial");
        assertThat(repository.find("[",0).getTotalElements()).isZero();
    }
    @Test void savesSevenFieldsPreservesMetadataAndPreventsOverwrite() {
        ObjectId id = new ObjectId();
        mongo.getCollection("products").insertOne(new Document("_id",id).append("asin","ASIN")
                .append("status","NEED_PRODUCT_INFO").append("mention_count",12)
                .append("ad_ratio",0.25).append("ad_likelihood_sum",3.0)
                .append("name",null).append("name_kr",null).append("brand",null).append("category",null)
                .append("thumbnail_url",null).append("description",null).append("product_url",null)
                .append("custom_metadata",new Document("keep",true)));
        assertThat(repository.get(id.toHexString()).asin()).isEqualTo("ASIN");
        assertThat(repository.find(id.toHexString(),0).getTotalElements()).isEqualTo(1);
        repository.save(id.toHexString(), request());
        Document stored = mongo.getCollection("products").find(new Document("_id",id)).first();
        assertThat(stored.getString("name_kr")).isEqualTo("한글명");
        assertThat(stored.getString("thumbnail_url")).isEqualTo("https://cdn.example.com/image.png");
        assertThat(stored.getString("product_url")).isEqualTo("https://shop.example.com/link");
        assertThat(stored.get("custom_metadata")).isEqualTo(new Document("keep",true));
        assertThat(stored.getString("status")).isEqualTo("PENDING");
        assertThat(stored.get("price", org.bson.types.Decimal128.class).bigDecimalValue()).isEqualByComparingTo("19.99");
        assertThat(stored.getInteger("mention_count")).isEqualTo(12);
        assertThat(stored.getDouble("ad_ratio")).isEqualTo(0.25);
        assertThat(stored.getDouble("ad_likelihood_sum")).isEqualTo(3.0);
        assertThat(repository.find("",0).getTotalElements()).isZero();
        assertThatThrownBy(() -> repository.save(id.toHexString(),request())).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void handlesStringIdsAndClampsLastPageAfterQueueShrinks() {
        for (int i=0; i<26; i++) mongo.getCollection("products").insertOne(new Document("_id","p"+i).append("asin","B"+i).append("status","NEED_PRODUCT_INFO"));
        assertThat(repository.find("",1).getContent()).hasSize(1);
        String id = repository.find("",1).getContent().get(0).id();
        repository.save(id,request());
        assertThat(repository.find("",1).getNumber()).isZero();
        assertThat(repository.find("",-10).getContent()).hasSize(25);
    }
    @Test void onlyFillsMissingFieldsAndPreservesExistingValuesIncludingZero() {
        var stored = new Document("_id", "partial").append("asin", "B001").append("status", "NEED_PRODUCT_INFO")
                .append("name", "Original").append("brand", "Existing brand").append("category", "toners")
                .append("price", 0).append("thumbnail_url", "https://cdn.example.com/original.jpg")
                .append("description", " ").append("mention_count", 25);
        mongo.getCollection("products").insertOne(stored);
        assertThat(repository.get("partial").missing("price")).isFalse();
        repository.save("partial", request());
        var after = mongo.getCollection("products").find(new Document("_id", "partial")).first();
        for (String field : List.of("name", "brand", "category", "price", "thumbnail_url", "mention_count"))
            assertThat(after.get(field)).isEqualTo(stored.get(field));
        assertThat(after.getString("description")).isEqualTo("설명");
        assertThat(after.getString("status")).isEqualTo("PENDING");
    }
    @Test void incompleteRequestCannotAdvanceStatus() {
        mongo.getCollection("products").insertOne(new Document("_id", "incomplete").append("status", "NEED_PRODUCT_INFO"));
        assertThatThrownBy(() -> repository.save("incomplete", new ProductEnrichmentRequest()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(mongo.getCollection("products").find(new Document("_id", "incomplete")).first().getString("status"))
                .isEqualTo("NEED_PRODUCT_INFO");
    }
    ProductEnrichmentRequest request() {
        var r = new ProductEnrichmentRequest();
        r.setPrice(new java.math.BigDecimal("19.99")); r.setName("Name"); r.setNameKo("한글명"); r.setBrand("Brand"); r.setCategory("toners");
        r.setDescription("설명"); r.setThumbnailUrl("https://cdn.example.com/image.png"); r.setProductUrl("https://shop.example.com/link");
        return r;
    }
}
