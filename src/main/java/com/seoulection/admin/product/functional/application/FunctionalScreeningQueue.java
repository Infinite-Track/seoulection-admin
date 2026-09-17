package com.seoulection.admin.product.functional.application;

import com.seoulection.admin.product.application.dto.ProductResult;
import com.seoulection.admin.product.application.service.ProductService;
import com.seoulection.admin.product.domain.enums.ProductStatus;
import com.seoulection.admin.product.functional.domain.ScreeningOutcome;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/** Durable bounded queue. A PostgreSQL session lock allows only one global worker. */
@Service
public class FunctionalScreeningQueue {
    private static final long WORKER_LOCK = 87264103L;
    private static final long ENQUEUE_LOCK = 87264104L;
    private final JdbcTemplate jdbc;
    private final ProductService products;
    private final FunctionalScreeningService screening;
    private final FunctionalScreeningQueueSettings settings;
    private final org.springframework.transaction.support.TransactionTemplate transactions;
    private final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(getClass());

    public FunctionalScreeningQueue(JdbcTemplate jdbc, ProductService products,
                                   FunctionalScreeningService screening,
                                   org.springframework.transaction.PlatformTransactionManager transactionManager,
                                   FunctionalScreeningQueueSettings settings) {
        this.jdbc = jdbc; this.products = products; this.screening = screening;
        this.settings = settings;
        this.transactions = new org.springframework.transaction.support.TransactionTemplate(transactionManager);
    }

    public static boolean eligible(ProductResult p) {
        return p.status() == ProductStatus.INGREDIENTS_ADDED && p.nameKo() != null && !p.nameKo().isBlank();
    }

    public static String fingerprint(ProductResult p) {
        try {
            String input = String.join("\u0000", p.id(), text(p.nameKo()), text(p.brand()), text(p.category()));
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    private static String text(String v) { return v == null ? "" : v; }

    public String enqueue(String id, boolean force) {
        return transactions.execute(tx -> enqueueInTransaction(id, force));
    }

    private String enqueueInTransaction(String id, boolean force) {
        if (!screening.isEnabled()) return "자동 조회가 꺼져 있습니다.";
        ProductResult p = products.getProduct(id);
        if (!eligible(p)) return "기능성 검수 대상이며 한글 이름이 입력된 제품만 조회할 수 있습니다.";
        if (!Boolean.TRUE.equals(jdbc.queryForObject("SELECT pg_try_advisory_xact_lock(?)", Boolean.class, ENQUEUE_LOCK)))
            return "다른 작업을 등록 중입니다. 잠시 후 다시 시도해 주세요.";
        var jobs = jdbc.queryForList("SELECT status, fingerprint, updated_at FROM functional_screening_jobs WHERE product_id=?", id);
        String hash = fingerprint(p);
        if (!jobs.isEmpty()) {
            var job = jobs.get(0);
            if (List.of("QUEUED", "RUNNING").contains(job.get("status"))) return "이미 대기 또는 조회 중입니다.";
            if (hash.equals(job.get("fingerprint")) && !force) return "이미 조회한 입력입니다.";
            // Prevent repeated manual retries from exhausting the external API quota.
            if (force && jdbc.queryForObject("SELECT updated_at > CURRENT_TIMESTAMP - INTERVAL '1 minute' FROM functional_screening_jobs WHERE product_id=?", Boolean.class, id))
                return "다시 조회는 이전 작업 종료 후 1분 뒤에 가능합니다.";
        } else if (!force && screening.find(id).isPresent()) {
            // Adopt legacy results without re-querying every previously reviewed product.
            jdbc.update("INSERT INTO functional_screening_jobs(product_id,fingerprint,status,message,finished_at) VALUES (?,?,'COMPLETED','기존 조회 결과를 사용합니다.',CURRENT_TIMESTAMP)", id, hash);
            return "기존 조회 결과가 있습니다. 입력이 변경됐다면 다시 조회해 주세요.";
        }
        Long size = jdbc.queryForObject("SELECT count(*) FROM functional_screening_jobs WHERE status IN ('QUEUED','RUNNING')", Long.class);
        if (size != null && size >= settings.get().capacity()) return "대기열이 가득 찼습니다. 잠시 후 다시 시도해 주세요.";
        jdbc.update("""
            INSERT INTO functional_screening_jobs(product_id,fingerprint,status) VALUES (?,?,'QUEUED')
            ON CONFLICT(product_id) DO UPDATE SET fingerprint=EXCLUDED.fingerprint,status='QUEUED',
            message=NULL,queued_at=CURRENT_TIMESTAMP,started_at=NULL,finished_at=NULL,updated_at=CURRENT_TIMESTAMP
            """, id, hash);
        return "조회 대기열에 등록했습니다. 다른 제품으로 이동해도 계속 처리됩니다.";
    }

    public Map<String, Object> status(String id) {
        var rows = jdbc.queryForList("SELECT status,message FROM functional_screening_jobs WHERE product_id=?", id);
        if (rows.isEmpty()) return Map.of("status", "NONE", "message", "아직 등록된 작업이 없습니다.");
        var row = rows.get(0);
        Long ahead = jdbc.queryForObject("""
            SELECT count(*) FROM functional_screening_jobs j, functional_screening_jobs mine
            WHERE mine.product_id=? AND j.product_id<>mine.product_id
              AND (j.status='RUNNING' OR (j.status='QUEUED' AND (j.queued_at,j.product_id)<(mine.queued_at,mine.product_id)))
            """, Long.class, id);
        return Map.of("status", row.get("status"), "message", row.get("message") == null ? "" : row.get("message"), "ahead", ahead == null ? 0 : ahead);
    }

    public int enqueueEligible(int limit) {
        int count = 0;
        int bounded = Math.min(Math.max(limit, 1), settings.get().capacity());
        // Walk all pages so completed jobs near the beginning cannot starve later products.
        for (int page = 0; count < bounded; page++) {
            var batch = products.getFunctionalScreeningProductsOldestFirst(page, 50);
            if (batch == null || batch.isEmpty()) break;
            for (var p : batch.content()) {
                if (eligible(p) && enqueue(p.id(), false).startsWith("조회 대기열에 등록")) count++;
                if (count >= bounded) break;
            }
            if (!batch.hasNext()) break;
        }
        return count;
    }

    @Scheduled(fixedDelay = 15000, initialDelayString = "${admin.functional-screening.queue.initial-delay-ms:30000}")
    public void scan() {
        if (!screening.isEnabled()) return;
        try {
            if (settings.claimScan()) enqueueEligible(settings.get().capacity());
        } catch (RuntimeException e) { log.warn("기능성 큐 등록 실패", e); }
    }

    @Scheduled(fixedDelayString = "${admin.functional-screening.queue.worker-delay-ms:5000}", initialDelayString = "${admin.functional-screening.queue.initial-delay-ms:30000}")
    public void work() {
        if (!screening.isEnabled()) return;
        try {
            jdbc.execute((ConnectionCallback<Void>) connection -> {
                boolean locked;
                try (var s = connection.prepareStatement("SELECT pg_try_advisory_lock(?)")) {
                    s.setLong(1, WORKER_LOCK);
                    try (var r = s.executeQuery()) { r.next(); locked = r.getBoolean(1); }
                }
                if (!locked) return null;
                try {
                    // With exclusive global ownership, RUNNING rows are leftovers from a crashed worker.
                    jdbc.update("UPDATE functional_screening_jobs SET status='FAILED',message='작업이 중단되었습니다. 다시 조회해 주세요.',finished_at=CURRENT_TIMESTAMP,updated_at=CURRENT_TIMESTAMP WHERE status='RUNNING'");
                    var jobs = jdbc.queryForList("""
                        UPDATE functional_screening_jobs SET status='RUNNING',attempts=attempts+1,
                        started_at=CURRENT_TIMESTAMP,updated_at=CURRENT_TIMESTAMP
                        WHERE product_id=(SELECT product_id FROM functional_screening_jobs WHERE status='QUEUED' ORDER BY queued_at,product_id LIMIT 1)
                        RETURNING product_id,fingerprint
                        """);
                    if (jobs.isEmpty()) return null;
                    String id = (String) jobs.get(0).get("product_id"), hash = (String) jobs.get(0).get("fingerprint");
                    try {
                        ProductResult p = products.getProduct(id);
                        if (!eligible(p) || !fingerprint(p).equals(hash)) { finish(id, "CANCELLED", "입력 또는 검수 상태가 변경되어 취소했습니다."); return null; }
                        var result = screening.screen(p, () -> {
                            ProductResult current = products.getProduct(id);
                            return eligible(current) && fingerprint(current).equals(hash);
                        });
                        ProductResult current = products.getProduct(id);
                        if (!eligible(current) || !fingerprint(current).equals(hash)) finish(id, "CANCELLED", "관리자 저장 또는 입력 변경으로 결과를 적용하지 않았습니다.");
                        else finish(id, result.outcome() == ScreeningOutcome.FAILED ? "FAILED" : "COMPLETED", result.reason());
                    } catch (RuntimeException e) {
                        log.warn("기능성 작업 실패 productId={}", id, e);
                        finish(id, "FAILED", "조회에 실패했습니다. 잠시 후 다시 조회해 주세요.");
                    }
                } finally {
                    try (var s = connection.prepareStatement("SELECT pg_advisory_unlock(?)")) { s.setLong(1, WORKER_LOCK); s.execute(); }
                }
                return null;
            });
        } catch (RuntimeException e) { log.warn("기능성 워커 실패", e); }
    }

    private void finish(String id, String status, String message) {
        jdbc.update("UPDATE functional_screening_jobs SET status=?,message=?,finished_at=CURRENT_TIMESTAMP,updated_at=CURRENT_TIMESTAMP WHERE product_id=?", status, message, id);
    }
}
