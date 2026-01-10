package team.carrypigeon.backend.api.chat.domain.flow;

import org.junit.jupiter.api.Test;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class CPFlowContextTests {

    @Test
    void select_queryFnNull_returnsNull() {
        CPFlowContext context = new CPFlowContext();
        assertNull(context.select("k", null));
    }

    @Test
    void select_cacheKeyBlank_doesNotCache() {
        CPFlowContext context = new CPFlowContext();
        AtomicInteger calls = new AtomicInteger();

        Integer first = context.select("", () -> calls.incrementAndGet());
        Integer second = context.select("", () -> calls.incrementAndGet());

        assertEquals(1, first);
        assertEquals(2, second);
        assertEquals(2, calls.get());
    }

    @Test
    void select_cacheKeyPresent_cachesNonNullResult() {
        CPFlowContext context = new CPFlowContext();
        AtomicInteger calls = new AtomicInteger();

        String first = context.select("user-id:1", () -> "v" + calls.incrementAndGet());
        String second = context.select("user-id:1", () -> "v" + calls.incrementAndGet());

        assertEquals("v1", first);
        assertEquals("v1", second);
        assertEquals(1, calls.get());
    }

    @Test
    void select_doesNotCacheNull() {
        CPFlowContext context = new CPFlowContext();
        AtomicInteger calls = new AtomicInteger();

        assertNull(context.select("k", () -> {
            calls.incrementAndGet();
            return null;
        }));
        assertNull(context.select("k", () -> {
            calls.incrementAndGet();
            return null;
        }));
        assertEquals(2, calls.get());
    }

    @Test
    void select_debugEnabled_shouldExecuteDebugLogBranches() {
        Configurator.setLevel(CPFlowContext.class.getName(), Level.DEBUG);

        CPFlowContext context = new CPFlowContext();
        AtomicInteger calls = new AtomicInteger();

        assertEquals("v1", context.select("k", () -> "v" + calls.incrementAndGet()));
        assertEquals("v1", context.select("k", () -> "v" + calls.incrementAndGet()));
        assertEquals(1, calls.get());
    }
}
