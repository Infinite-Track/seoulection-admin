package com.seoulection.admin.trending;

import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;

@Service
public class TrendingService {
    public static class Settings {
        private int recentDays, recentWeight, totalWeight, oliveyoungWeight, maxProducts;
        private double top3Score=1.0, top10Score=.85, top30Score=.60, top100Score=.30;
        public Settings() {}
        public Settings(int recentDays,int recentWeight,int totalWeight,int oliveyoungWeight,int maxProducts){this.recentDays=recentDays;this.recentWeight=recentWeight;this.totalWeight=totalWeight;this.oliveyoungWeight=oliveyoungWeight;this.maxProducts=maxProducts;}
        public int recentDays(){return recentDays;} public int recentWeight(){return recentWeight;} public int totalWeight(){return totalWeight;} public int oliveyoungWeight(){return oliveyoungWeight;} public int maxProducts(){return maxProducts;}
        public int getRecentDays(){return recentDays;} public void setRecentDays(int v){recentDays=v;}
        public int getRecentWeight(){return recentWeight;} public void setRecentWeight(int v){recentWeight=v;}
        public int getTotalWeight(){return totalWeight;} public void setTotalWeight(int v){totalWeight=v;}
        public int getOliveyoungWeight(){return oliveyoungWeight;} public void setOliveyoungWeight(int v){oliveyoungWeight=v;}
        public int getMaxProducts(){return maxProducts;} public void setMaxProducts(int v){maxProducts=v;}
        public double getTop3Score(){return top3Score;} public void setTop3Score(double v){top3Score=v;}
        public double getTop10Score(){return top10Score;} public void setTop10Score(double v){top10Score=v;}
        public double getTop30Score(){return top30Score;} public void setTop30Score(double v){top30Score=v;}
        public double getTop100Score(){return top100Score;} public void setTop100Score(double v){top100Score=v;}
    }
    public record Result(String productId, String name, int recentMentions, long totalMentions,
                         Integer oliveyoungRank, double recentScore, double totalScore,
                         double oliveyoungScore, double score, int rank) {}
    public record ProductOption(String id, String name, String brand, String asin) {}

    private static final Settings DEFAULT = new Settings(30, 50, 10, 40, 20);
    private final JdbcTemplate jdbc;
    private final MongoTemplate mongo;
    private final ObjectMapper json;

    public TrendingService(JdbcTemplate jdbc, MongoTemplate mongo, ObjectMapper json) {
        this.jdbc = jdbc; this.mongo = mongo; this.json = json;
    }

    public Settings activeSettings() {
        return jdbc.query("select criteria::text, max_products from trending_policy where status='ACTIVE' limit 1",
                rs -> rs.next() ? parse(rs.getString(1), rs.getInt(2)) : DEFAULT);
    }

    public List<Map<String,Object>> oliveyoungSignals() {
        return jdbc.queryForList("""
                select s.id, s.product_id, c.name, c.brand, c.asin, s.rank, s.measured_at
                from trending_external_signal s join products_catalog c on c.id=s.product_id
                where s.source='OLIVEYOUNG' and s.active=true
                order by s.measured_at desc, s.rank asc
                """);
    }

    public List<ProductOption> searchProducts(String query) {
        if (query == null || query.isBlank()) return List.of();
        String pattern = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
        return jdbc.query("""
                select id, name, brand, asin
                from products_catalog
                where lower(coalesce(name, '') || ' ' || coalesce(brand, '') || ' ' || coalesce(asin, '')) like ?
                order by name, brand, asin
                limit 20
                """, (rs, rowNum) -> new ProductOption(
                rs.getString("id"), rs.getString("name"), rs.getString("brand"), rs.getString("asin")), pattern);
    }

    public void saveOliveyoung(String productId, int rank, LocalDate measuredAt) {
        if (productId == null || productId.isBlank()) throw new IllegalArgumentException("제품을 검색해 선택해 주세요.");
        if (rank < 1 || rank > 100) throw new IllegalArgumentException("올리브영 순위는 1~100위까지 입력해 주세요.");
        Integer exists = jdbc.queryForObject("select count(*) from products_catalog where id=?", Integer.class, productId);
        if (exists == null || exists == 0) throw new IllegalArgumentException("선택한 제품을 찾을 수 없습니다.");
        jdbc.update("""
                insert into trending_external_signal(product_id,source,rank,measured_at,active)
                values (?, 'OLIVEYOUNG', ?, ?, true)
                on conflict(product_id,source,measured_at)
                do update set rank=excluded.rank,active=true,updated_at=current_timestamp
                """, productId, rank, measuredAt);
    }

    public List<Result> preview(Settings settings) { validate(settings); return calculate(settings); }

    @Transactional
    public List<Result> apply(Settings settings, String actor) {
        validate(settings);
        int version = jdbc.queryForObject("select coalesce(max(version),0)+1 from trending_policy", Integer.class);
        String criteria = criteriaJson(settings);
        jdbc.update("insert into trending_policy(version,status,criteria,max_products,created_at) values (?, 'DRAFT', cast(? as jsonb), ?, current_timestamp)",
                version, criteria, settings.maxProducts());
        List<Result> results = calculate(settings);
        insertResults(version, settings, results);
        jdbc.update("update trending_policy set status='ARCHIVED' where status='ACTIVE'");
        jdbc.update("update trending_policy set status='ACTIVE',applied_at=current_timestamp,applied_by=? where version=?", actor, version);
        return results;
    }

    @Scheduled(cron = "0 0 12 * * *", zone = "Asia/Seoul")
    @Transactional
    public void recalculateAtNoon() {
        Integer version = jdbc.query("select version from trending_policy where status='ACTIVE' limit 1", rs -> rs.next() ? rs.getInt(1) : null);
        if (version == null) return;
        Settings settings = activeSettings();
        List<Result> results = calculate(settings);
        jdbc.update("delete from product_trending where policy_version=?", version);
        insertResults(version, settings, results);
    }

    private void insertResults(int version, Settings settings, List<Result> results) {
        for (Result r : results) jdbc.update("""
                insert into product_trending(product_id,policy_version,recent_period_days,recent_mention_count,
                  total_mention_count,oliveyoung_rank,criterion_scores,trending_score,rank)
                values (?,?,?,?,?,?,cast(? as jsonb),?,?)
                """, r.productId(), version, settings.recentDays(), r.recentMentions(), r.totalMentions(),
                r.oliveyoungRank(), scoresJson(r), BigDecimal.valueOf(r.score()), r.rank());
    }

    private List<Result> calculate(Settings s) {
        List<Map<String,Object>> products = jdbc.queryForList("select id,name,coalesce(mention_count,0) mention_count from products_catalog");
        Map<String,String> idByAsin = new HashMap<>();
        for (Document p : mongo.getCollection("products").find()) {
            String asin=value(p,"asin"), id=value(p,"id","_id"); if(asin!=null&&id!=null) idByAsin.put(asin,id);
        }
        Instant cutoff = Instant.now().minus(Duration.ofDays(s.recentDays()));
        Map<String,Instant> published = new HashMap<>();
        for (Document v : mongo.getCollection("videos").find()) {
            String videoId = value(v, "video_id", "youtube_video_id", "id", "_id");
            Instant date = instant(v.get("published_at"));
            if (videoId != null && date != null) published.put(videoId, date);
        }
        Map<String,Set<String>> recentVideos = new HashMap<>();
        for (Document review : mongo.getCollection("reviews").find()) {
            String videoId = value(review, "video_id");
            Instant date = published.get(videoId);
            if (date == null || date.isBefore(cutoff)) continue;
            String productId = value(review, "product_id");
            if (productId == null) productId = idByAsin.get(value(review, "asin"));
            if (productId != null) recentVideos.computeIfAbsent(productId, ignored -> new HashSet<>()).add(videoId);
        }
        Map<String,Integer> olive = new HashMap<>();
        jdbc.query("""
                select distinct on(product_id) product_id,rank from trending_external_signal
                where source='OLIVEYOUNG' and active=true order by product_id,measured_at desc,updated_at desc
                """, rs -> { olive.put(rs.getString(1), rs.getInt(2)); });
        List<Integer> recents = products.stream().map(p -> recentVideos.getOrDefault(p.get("id").toString(), Set.of()).size()).toList();
        List<Long> totals = products.stream().map(p -> ((Number)p.get("mention_count")).longValue()).toList();
        double recentP95 = Math.max(percentile95(recents.stream().map(Number.class::cast).toList()), 1);
        double totalP95 = Math.max(percentile95(totals.stream().map(Number.class::cast).toList()), 1);
        List<Result> unsorted = new ArrayList<>();
        for (var p : products) {
            String id = p.get("id").toString(); int rc = recentVideos.getOrDefault(id, Set.of()).size();
            long tc = ((Number)p.get("mention_count")).longValue(); Integer or = olive.get(id);
            double rs = Math.min(rc / recentP95, 1), ts = Math.min(tc / totalP95, 1), os = oliveScore(s, or);
            double score = rs*s.recentWeight()/100d + ts*s.totalWeight()/100d + os*s.oliveyoungWeight()/100d;
            unsorted.add(new Result(id, Objects.toString(p.get("name"), id), rc, tc, or, rs, ts, os, score, 0));
        }
        unsorted.sort(Comparator.comparingDouble(Result::score).reversed().thenComparing(Result::productId));
        List<Result> ranked = new ArrayList<>();
        for (int i=0; i<Math.min(s.maxProducts(), unsorted.size()); i++) { Result r=unsorted.get(i); ranked.add(new Result(r.productId,r.name,r.recentMentions,r.totalMentions,r.oliveyoungRank,r.recentScore,r.totalScore,r.oliveyoungScore,r.score,i+1)); }
        return ranked;
    }

    static double percentile95(List<Number> values) {
        if (values.isEmpty()) return 0;
        double[] sorted = values.stream().mapToDouble(Number::doubleValue).sorted().toArray();
        double index = .95 * (sorted.length - 1); int low=(int)Math.floor(index), high=(int)Math.ceil(index);
        return sorted[low] + (sorted[high]-sorted[low])*(index-low);
    }
    private static double oliveScore(Settings s,Integer rank) { if(rank==null)return 0; if(rank<=3)return s.top3Score; if(rank<=10)return s.top10Score; if(rank<=30)return s.top30Score; if(rank<=100)return s.top100Score; return 0; }
    private static void validate(Settings s) { if (!Set.of(7,14,30).contains(s.recentDays())) throw new IllegalArgumentException("최근 기간은 7·14·30일 중 선택해 주세요."); if(s.recentWeight()+s.totalWeight()+s.oliveyoungWeight()!=100) throw new IllegalArgumentException("가중치 합계는 100%여야 합니다."); if(s.maxProducts()<1||s.maxProducts()>100) throw new IllegalArgumentException("노출 개수는 1~100이어야 합니다."); if(s.top3Score<0||s.top3Score>1||s.top10Score<0||s.top10Score>1||s.top30Score<0||s.top30Score>1||s.top100Score<0||s.top100Score>1||s.top3Score<s.top10Score||s.top10Score<s.top30Score||s.top30Score<s.top100Score) throw new IllegalArgumentException("올리브영 점수는 0~1이며 높은 순위 구간의 점수가 더 커야 합니다."); }
    private String criteriaJson(Settings s) { try { return json.writeValueAsString(Map.of("recent_mentions",Map.of("enabled",true,"weight",s.recentWeight()/100d,"config",Map.of("days",s.recentDays())),"total_mentions",Map.of("enabled",true,"weight",s.totalWeight()/100d),"oliveyoung_rank",Map.of("enabled",true,"weight",s.oliveyoungWeight()/100d,"config",Map.of("rankBands",List.of(Map.of("max",3,"score",s.top3Score),Map.of("max",10,"score",s.top10Score),Map.of("max",30,"score",s.top30Score),Map.of("max",100,"score",s.top100Score)))))); } catch(Exception e){throw new IllegalStateException(e);} }
    private String scoresJson(Result r) { try{return json.writeValueAsString(Map.of("recent_mentions",round(r.recentScore),"total_mentions",round(r.totalScore),"oliveyoung_rank",round(r.oliveyoungScore)));}catch(Exception e){throw new IllegalStateException(e);} }
    private Settings parse(String raw,int max) { try { var n=json.readTree(raw); var s=new Settings(n.get("recent_mentions").get("config").get("days").asInt(),(int)Math.round(n.get("recent_mentions").get("weight").asDouble()*100),(int)Math.round(n.get("total_mentions").get("weight").asDouble()*100),(int)Math.round(n.get("oliveyoung_rank").get("weight").asDouble()*100),max);var b=n.get("oliveyoung_rank").get("config").get("rankBands");s.top3Score=b.get(0).get("score").asDouble();s.top10Score=b.get(1).get("score").asDouble();s.top30Score=b.get(2).get("score").asDouble();s.top100Score=b.get(3).get("score").asDouble();return s;}catch(Exception e){return DEFAULT;} }
    private static String value(Document d,String... keys){for(String k:keys){Object v=d.get(k);if(v!=null)return v.toString();}return null;}
    private static Instant instant(Object v){if(v instanceof Date d)return d.toInstant();if(v instanceof Instant i)return i;if(v instanceof String s)try{return Instant.parse(s);}catch(Exception ignored){}return null;}
    private static double round(double v){return BigDecimal.valueOf(v).setScale(5,RoundingMode.HALF_UP).doubleValue();}
}
