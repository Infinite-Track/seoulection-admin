package com.seoulection.admin.product.functional.application;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;

@Service
public class FunctionalScreeningQueueSettings {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    public FunctionalScreeningQueueSettings(JdbcTemplate jdbc, PlatformTransactionManager transactionManager) {
        this.jdbc = jdbc;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    public record Settings(boolean autoEnqueue, int scanIntervalMinutes, int capacity) {}

    public Settings get() {
        return jdbc.queryForObject("SELECT auto_enqueue,scan_interval_minutes,capacity FROM functional_screening_queue_settings WHERE id=1",
                (rs, n) -> new Settings(rs.getBoolean(1), rs.getInt(2), rs.getInt(3)));
    }

    public void update(boolean autoEnqueue, int interval, int capacity) {
        if (interval < 1 || interval > 1440) throw new IllegalArgumentException("자동 등록 주기는 1~1440분으로 입력해 주세요.");
        if (capacity < 1 || capacity > 1000) throw new IllegalArgumentException("대기열 한도는 1~1000개로 입력해 주세요.");
        transactions.executeWithoutResult(tx -> {
            // Serialize limit changes with job admission, including across admin instances.
            jdbc.execute("SELECT pg_advisory_xact_lock(87264104)");
            jdbc.update("UPDATE functional_screening_queue_settings SET auto_enqueue=?,scan_interval_minutes=?,capacity=?,last_scan_at=NULL,updated_at=CURRENT_TIMESTAMP WHERE id=1", autoEnqueue, interval, capacity);
        });
    }

    /** Atomic persistent due check: only one instance starts each registration scan. */
    public boolean claimScan() {
        return jdbc.update("""
            UPDATE functional_screening_queue_settings SET last_scan_at=CURRENT_TIMESTAMP
            WHERE id=1 AND auto_enqueue=TRUE
              AND (last_scan_at IS NULL OR last_scan_at <= CURRENT_TIMESTAMP - scan_interval_minutes * INTERVAL '1 minute')
            """) == 1;
    }

    public long activeCount() {
        Long count = jdbc.queryForObject("SELECT count(*) FROM functional_screening_jobs WHERE status IN ('QUEUED','RUNNING')", Long.class);
        return count == null ? 0 : count;
    }
}
