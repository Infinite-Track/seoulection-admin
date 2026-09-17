package com.seoulection.admin.product.functional.application;

import com.seoulection.admin.product.application.dto.ProductPage;
import com.seoulection.admin.product.application.dto.ProductResult;
import com.seoulection.admin.product.infrastructure.document.ProductDocument;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Sort;
import java.util.*;
import java.util.regex.Pattern;

/** Joins lightweight Mongo identities with batched PG jobs before filtering/pagination. */
@Service
public class FunctionalScreeningList {
    public enum State {
        COMPLETED("조회 완료", "success"), RUNNING("조회 중", "info"), QUEUED("조회 대기", "warning"),
        NAME_REQUIRED("이름 입력 필요", "warning"), UNREGISTERED("등록 대기", "neutral"), FAILED("조회 실패", "danger");
        private final String label, tone;
        State(String label, String tone) { this.label=label; this.tone=tone; }
        public String label() { return label; }
        public String tone() { return tone; }
    }
    public record Result(ProductPage page, Map<String, State> states, Map<State, Long> counts) {}
    private final MongoTemplate mongo;
    private final NamedParameterJdbcTemplate jdbc;
    public FunctionalScreeningList(MongoTemplate mongo, org.springframework.jdbc.core.JdbcTemplate jdbc) {
        this.mongo=mongo; this.jdbc=new NamedParameterJdbcTemplate(jdbc);
    }
    public static State classify(String nameKo, String job) {
        if(nameKo==null || nameKo.isBlank()) return State.NAME_REQUIRED;
        if(job==null) return State.UNREGISTERED;
        return switch(job) {
            case "COMPLETED" -> State.COMPLETED;
            case "RUNNING" -> State.RUNNING;
            case "QUEUED" -> State.QUEUED;
            case "FAILED" -> State.FAILED;
            default -> State.UNREGISTERED;
        };
    }
    public Result find(String keyword, String filter, int page, int size) {
        return find(keyword, filter, page, size, "oldest");
    }
    public Result find(String keyword, String filter, int page, int size, String order) {
        Query query=new Query(Criteria.where("status").is("INGREDIENTS_ADDED"));
        // Keep positions stable as screening jobs change state; ID breaks timestamp ties.
        query.with(Sort.by("newest".equals(order) ? Sort.Direction.DESC : Sort.Direction.ASC, "created_at", "_id"));
        if(keyword!=null && !keyword.isBlank()) {
            String escaped=Pattern.quote(keyword.trim());
            query.addCriteria(new Criteria().orOperator(Criteria.where("name").regex(escaped,"i"),Criteria.where("brand").regex(escaped,"i")));
        }
        query.fields().include("_id").include("name_kr");
        List<Document> identities=mongo.find(query,Document.class,"products");
        Map<String,String> jobs=new HashMap<>();
        List<String> ids=identities.stream().map(d->d.get("_id").toString()).toList();
        for(int offset=0;offset<ids.size();offset+=500) {
            jdbc.query("SELECT product_id,status FROM functional_screening_jobs WHERE product_id IN (:ids)",
                Map.of("ids",ids.subList(offset,Math.min(offset+500,ids.size()))),rs->{jobs.put(rs.getString(1),rs.getString(2));});
        }
        Map<String,State> states=new HashMap<>();
        Map<State,Long> counts=new EnumMap<>(State.class);
        for(State state:State.values()) counts.put(state,0L);
        for(Document d:identities) {
            String id=d.get("_id").toString();
            State state=classify(d.getString("name_kr"),jobs.get(id));
            states.put(id,state); counts.merge(state,1L,Long::sum);
        }
        List<Document> matches=identities.stream()
            .filter(d->filter==null || filter.isEmpty() || states.get(d.get("_id").toString()).name().equals(filter))
            .toList();
        int total=matches.size(), pages=(total+size-1)/size;
        int current=Math.min(Math.max(page,0),Math.max(pages-1,0));
        List<Object> pageIds=matches.subList(current*size,Math.min((current+1)*size,total)).stream().map(d->d.get("_id")).toList();
        Map<String,ProductResult> products=new HashMap<>();
        if(!pageIds.isEmpty()) for(ProductDocument d:mongo.find(new Query(Criteria.where("_id").in(pageIds)),ProductDocument.class)) {
            var p=ProductResult.from(d.toDomain()); products.put(p.id(),p);
        }
        var content=pageIds.stream().map(id->products.get(id.toString())).filter(Objects::nonNull).toList();
        return new Result(new ProductPage(content,current,size,total,pages),states,counts);
    }
}
