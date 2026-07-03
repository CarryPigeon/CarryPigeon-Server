package team.carrypigeon.backend.infrastructure.service.database.impl.testsupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Tests the env test data lifecycle helper.
 * Contract: env tests receive stable namespaced data keys and cleanup failures are never hidden.
 */
@Tag("unit")
class EnvTestDataScopeTests {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-07-03T04:40:16Z"), ZoneOffset.UTC);

    /**
     * Verifies that env test data keys are deterministic, readable, and prefixed by a unique namespace.
     */
    @Test
    @DisplayName("create builds sanitized namespace and key")
    void create_dirtyCaseName_returnsSanitizedNamespaceAndKey() {
        EnvTestDataScope scope = EnvTestDataScope.create("DB User Flow", FIXED_CLOCK, "Run#01");

        assertEquals("it_20260703044016_db_user_flow_run_01", scope.namespace());
        assertEquals("it_20260703044016_db_user_flow_run_01_auth_account", scope.key("Auth Account"));
    }

    /**
     * Verifies that blank names are rejected so real-environment data can always be traced to a test case.
     */
    @Test
    @DisplayName("create rejects blank case name")
    void create_blankCaseName_throwsIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> EnvTestDataScope.create("  !!!  ", FIXED_CLOCK, "run"));

        assertEquals("caseName must contain visible characters", exception.getMessage());
    }

    /**
     * Verifies that cleanup runs in reverse creation order for dependent external data.
     */
    @Test
    @DisplayName("close executes cleanup in reverse order")
    void close_registeredCleanup_executesReverseOrder() {
        List<String> cleaned = new ArrayList<>();
        EnvTestDataScope scope = EnvTestDataScope.create("cleanup order", FIXED_CLOCK, "run");

        scope.cleanup("parent row", () -> cleaned.add("parent"));
        scope.cleanup("child row", () -> cleaned.add("child"));
        scope.close();

        assertEquals(List.of("child", "parent"), cleaned);
    }

    /**
     * Verifies that cleanup exceptions fail the env test instead of silently polluting external services.
     */
    @Test
    @DisplayName("close exposes cleanup failures")
    void close_cleanupFails_throwsIllegalStateException() {
        EnvTestDataScope scope = EnvTestDataScope.create("cleanup failure", FIXED_CLOCK, "run");
        scope.cleanup("cache key", () -> {
            throw new IllegalStateException("redis unavailable");
        });

        IllegalStateException exception = assertThrows(IllegalStateException.class, scope::close);

        assertEquals("failed to clean env test data: cache key", exception.getMessage());
        assertTrue(exception.getCause().getMessage().contains("redis unavailable"));
    }

    /**
     * Verifies that the lifecycle scope rejects new cleanup actions after it has been closed.
     */
    @Test
    @DisplayName("cleanup rejects registrations after close")
    void cleanup_afterClose_throwsIllegalStateException() {
        EnvTestDataScope scope = EnvTestDataScope.create("closed scope", FIXED_CLOCK, "run");
        scope.close();

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> scope.cleanup("late cleanup", () -> {
                }));

        assertEquals("env test data scope is already closed", exception.getMessage());
    }
}
