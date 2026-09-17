package com.seoulection.admin.product.functional.application;

import com.seoulection.admin.product.application.dto.ProductResult;
import com.seoulection.admin.product.application.service.ProductService;
import com.seoulection.admin.product.domain.enums.ProductStatus;
import com.seoulection.admin.product.functional.domain.FunctionalScreening;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@Testcontainers(disabledWithoutDocker = true)
class FunctionalScreeningQueueTest {
    @Container static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");
    JdbcTemplate jdbc;
    ProductService products;
    FunctionalScreeningService screening;
    FunctionalScreeningQueue queue;
    FunctionalScreeningQueueSettings settings;

    @BeforeEach void setup() {
        var ds = new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        jdbc = new JdbcTemplate(ds);
        jdbc.execute("DROP TABLE IF EXISTS functional_screening_jobs");
        jdbc.execute("DROP TABLE IF EXISTS functional_screening_queue_settings");
        jdbc.execute("CREATE TABLE functional_screening_queue_settings(id integer primary key,auto_enqueue boolean,scan_interval_minutes integer,capacity integer,last_scan_at timestamptz,updated_at timestamptz default current_timestamp)");
        jdbc.update("INSERT INTO functional_screening_queue_settings(id,auto_enqueue,scan_interval_minutes,capacity) VALUES (1,true,5,1)");
        jdbc.execute("""
            CREATE TABLE functional_screening_jobs(product_id varchar(64) primary key,fingerprint varchar(64) not null,
            status varchar(16) not null,message text,attempts integer not null default 0,
            queued_at timestamptz not null default current_timestamp,started_at timestamptz,finished_at timestamptz,
            updated_at timestamptz not null default current_timestamp)
            """);
        products = mock(ProductService.class); screening = mock(FunctionalScreeningService.class);
        when(screening.isEnabled()).thenReturn(true);
        when(screening.find(anyString())).thenReturn(Optional.empty());
        settings = new FunctionalScreeningQueueSettings(jdbc, new JdbcTransactionManager(ds));
        queue = new FunctionalScreeningQueue(jdbc, products, screening, new JdbcTransactionManager(ds), settings);
        when(products.getProduct("p1")).thenReturn(product("p1", "세럼", ProductStatus.INGREDIENTS_ADDED));
        when(products.getProduct("p2")).thenReturn(product("p2", "크림", ProductStatus.INGREDIENTS_ADDED));
    }
    static ProductResult product(String id, String nameKo, ProductStatus status) {
        return new ProductResult(id, null, "Product", nameKo, "Brand", "treatments", null,
                BigDecimal.ZERO, null, null, 0, BigDecimal.ZERO, null, null, List.of(), Map.of(), null, List.of(), status);
    }

    @Test void deduplicatesAndBoundsWithoutExternalCalls() {
        assertThat(queue.enqueue("p1", false)).contains("등록했습니다");
        assertThat(queue.enqueue("p1", true)).contains("이미 대기");
        assertThat(queue.enqueue("p2", false)).contains("가득");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM functional_screening_jobs", Integer.class)).isEqualTo(1);
        verify(screening, never()).screen(any(ProductResult.class), any());
    }

    @Test void completesOneJobAndRequiresExplicitRetryAfterFailure() {
        queue.enqueue("p1", false);
        when(screening.screen(any(ProductResult.class), any())).thenReturn(FunctionalScreening.failed("p1", "API failure"));
        queue.work();
        assertThat(queue.status("p1").get("status")).isEqualTo("FAILED");
        assertThat(queue.enqueue("p1", false)).contains("이미 조회");
        assertThat(queue.enqueue("p1", true)).contains("1분");
        queue.work();
        verify(screening, times(1)).screen(any(ProductResult.class), any());
    }

    @Test void skipsProductsAlreadyConfirmedByAdministrator() {
        queue.enqueue("p1", false);
        when(products.getProduct("p1")).thenReturn(product("p1", "세럼", ProductStatus.READY_FOR_INCIAPI));
        queue.work();
        assertThat(queue.status("p1").get("status")).isEqualTo("CANCELLED");
        verify(screening, never()).screen(any(ProductResult.class), any());
    }

    @Test void workerRespectsGlobalSessionLock() throws Exception {
        queue.enqueue("p1", false);
        try (var connection = jdbc.getDataSource().getConnection(); var statement = connection.createStatement()) {
            statement.execute("SELECT pg_advisory_lock(87264103)");
            queue.work();
            assertThat(queue.status("p1").get("status")).isEqualTo("QUEUED");
            statement.execute("SELECT pg_advisory_unlock(87264103)");
        }
        verify(screening, never()).screen(any(ProductResult.class), any());
    }

    @Test void successfulWorkCompletesWithoutFinalApproval() {
        queue.enqueue("p1", false);
        var result = mock(FunctionalScreening.class);
        when(result.outcome()).thenReturn(com.seoulection.admin.product.functional.domain.ScreeningOutcome.NEEDS_REVIEW);
        when(result.reason()).thenReturn("추천 후보를 확인해 주세요.");
        when(screening.screen(any(ProductResult.class), any())).thenReturn(result);
        queue.work();
        assertThat(queue.status("p1").get("status")).isEqualTo("COMPLETED");
        verify(products, never()).reviewFunction(anyString(), anyList());
    }

    @Test void interruptedWorkIsNotRetriedForever() {
        queue.enqueue("p1", false);
        jdbc.update("UPDATE functional_screening_jobs SET status='RUNNING' WHERE product_id='p1'");
        queue.work();
        assertThat(queue.status("p1").get("status")).isEqualTo("FAILED");
        verify(screening, never()).screen(any(ProductResult.class), any());
    }

    @Test void inputChangesProduceNewFingerprint() {
        assertThat(FunctionalScreeningQueue.fingerprint(product("p1", "세럼", ProductStatus.INGREDIENTS_ADDED)))
                .isNotEqualTo(FunctionalScreeningQueue.fingerprint(product("p1", "새 이름", ProductStatus.INGREDIENTS_ADDED)));
    }

    @Test void settingsChangeCapacityImmediatelyAndDoNotDeleteJobs() {
        queue.enqueue("p1", false);
        settings.update(false, 10, 2);
        assertThat(queue.enqueue("p2", false)).contains("등록했습니다");
        settings.update(false, 10, 1);
        assertThat(settings.activeCount()).isEqualTo(2);
        assertThat(settings.get().capacity()).isEqualTo(1);
    }

    @Test void automaticRegistrationDueCheckIsPersistentAndAtomic() {
        assertThat(settings.claimScan()).isTrue();
        assertThat(settings.claimScan()).isFalse();
        jdbc.update("UPDATE functional_screening_queue_settings SET last_scan_at=CURRENT_TIMESTAMP - INTERVAL '6 minutes'");
        assertThat(settings.claimScan()).isTrue();
        settings.update(false, 5, 1);
        assertThat(settings.claimScan()).isFalse();
        assertThat(queue.enqueue("p1", true)).contains("등록했습니다");
    }

    @Test void rejectsInvalidSettingsWithoutChangingStoredValues() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> settings.update(true, 0, 1)).isInstanceOf(IllegalArgumentException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> settings.update(true, 5, 1001)).isInstanceOf(IllegalArgumentException.class);
        assertThat(settings.get().scanIntervalMinutes()).isEqualTo(5);
        assertThat(settings.get().capacity()).isEqualTo(1);
    }
}
