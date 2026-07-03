package team.carrypigeon.backend.infrastructure.service.database.impl.testsupport;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Test support for real-environment data lifecycle.
 * Responsibility: provide a unique namespace and reverse-order cleanup stack for env tests.
 * Boundary: this class is test-only and does not create database, cache, storage, or mail data by itself.
 */
public final class EnvTestDataScope implements AutoCloseable {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
            .withZone(ZoneOffset.UTC);

    private final String namespace;
    private final Deque<CleanupAction> cleanupActions = new ArrayDeque<>();
    private boolean closed;

    private EnvTestDataScope(String namespace) {
        this.namespace = namespace;
    }

    /**
     * Creates a data scope with a unique namespace for one real-environment test case.
     *
     * @param caseName stable test case name used to identify data ownership in external services
     * @return lifecycle scope used by a single env test
     */
    public static EnvTestDataScope create(String caseName) {
        return create(caseName, Clock.systemUTC(), UUID.randomUUID().toString().substring(0, 8));
    }

    static EnvTestDataScope create(String caseName, Clock clock, String uniqueSuffix) {
        Objects.requireNonNull(clock, "clock must not be null");
        String safeCaseName = sanitize(caseName);
        String safeSuffix = sanitize(uniqueSuffix);
        if (safeCaseName.isBlank()) {
            throw new IllegalArgumentException("caseName must contain visible characters");
        }
        if (safeSuffix.isBlank()) {
            throw new IllegalArgumentException("uniqueSuffix must contain visible characters");
        }
        Instant now = Instant.now(clock);
        return new EnvTestDataScope("it_" + TIMESTAMP_FORMATTER.format(now) + "_" + safeCaseName + "_" + safeSuffix);
    }

    /**
     * Returns the unique namespace that must prefix all data created by the current env test.
     *
     * @return namespaced owner marker for database rows, cache keys, object keys, or mail targets
     */
    public String namespace() {
        return namespace;
    }

    /**
     * Builds a namespaced key for data created in a real external service.
     *
     * @param suffix scenario-specific suffix such as table role, cache key, or object key fragment
     * @return namespace-prefixed key safe for common database/cache/storage identifiers
     */
    public String key(String suffix) {
        String safeSuffix = sanitize(suffix);
        if (safeSuffix.isBlank()) {
            throw new IllegalArgumentException("suffix must contain visible characters");
        }
        return namespace + "_" + safeSuffix;
    }

    /**
     * Registers cleanup work for data created by the current test.
     * Cleanup actions execute in reverse registration order so dependent data can be removed first.
     *
     * @param description readable cleanup description used when cleanup fails
     * @param task cleanup work, for example deleting a row, cache key, or object
     */
    public void cleanup(String description, CleanupTask task) {
        ensureOpen();
        String safeDescription = Objects.requireNonNull(description, "description must not be null").trim();
        if (safeDescription.isEmpty()) {
            throw new IllegalArgumentException("description must contain visible characters");
        }
        cleanupActions.addFirst(new CleanupAction(safeDescription, Objects.requireNonNull(task, "task must not be null")));
    }

    /**
     * Executes registered cleanup work and exposes cleanup failures to the test runner.
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        RuntimeException failure = null;
        while (!cleanupActions.isEmpty()) {
            CleanupAction action = cleanupActions.removeFirst();
            try {
                action.task().run();
            } catch (Exception exception) {
                IllegalStateException wrapped = new IllegalStateException(
                        "failed to clean env test data: " + action.description(), exception);
                if (failure == null) {
                    failure = wrapped;
                } else {
                    failure.addSuppressed(wrapped);
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("env test data scope is already closed");
        }
    }

    private static String sanitize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+", "")
                .replaceAll("_+$", "");
    }

    @FunctionalInterface
    public interface CleanupTask {

        void run() throws Exception;
    }

    private record CleanupAction(String description, CleanupTask task) {
    }
}
