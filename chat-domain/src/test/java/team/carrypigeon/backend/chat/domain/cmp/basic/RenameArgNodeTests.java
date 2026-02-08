package team.carrypigeon.backend.chat.domain.cmp.basic;

import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RenameArgNodeTests {

    @Test
    void process_jsonScript_shouldCopyValues() throws Exception {
        TestableRenameArgNode node = new TestableRenameArgNode("{\"a\":\"b\",\"c\":\"d\"}");

        CPFlowContext context = new CPFlowContext();
        context.setData("a", 1);
        context.setData("c", "x");

        node.process(null, context);

        assertEquals(Integer.valueOf(1), context.getData("b"));
        assertEquals("x", context.getData("d"));
        assertEquals(Integer.valueOf(1), context.getData("a"));
    }

    @Test
    void process_invalidJson_shouldThrowArgsError() {
        TestableRenameArgNode node = new TestableRenameArgNode("not-json");

        CPFlowContext context = new CPFlowContext();
        CPProblemException ex = assertThrows(CPProblemException.class, () -> node.process(null, context));
        assertEquals(422, ex.getProblem().status());
        assertEquals("validation_failed", ex.getProblem().reason());
    }

    @Test
    void process_dataNotStringOrMap_shouldThrowArgsError() {
        TestableRenameArgNode node = new TestableRenameArgNode(123);

        CPFlowContext context = new CPFlowContext();
        CPProblemException ex = assertThrows(CPProblemException.class, () -> node.process(null, context));
        assertEquals(422, ex.getProblem().status());
        assertEquals("validation_failed", ex.getProblem().reason());
    }

    @Test
    void process_mapScript_shouldSkipNonStringEntries() throws Exception {
        Map<Object, Object> script = new LinkedHashMap<>();
        script.put("a", "b");
        script.put(1, "x");
        script.put("c", 2);

        TestableRenameArgNode node = new TestableRenameArgNode(script);

        CPFlowContext context = new CPFlowContext();
        context.setData("a", 1);
        context.setData("c", "x");

        node.process(null, context);

        assertEquals(Integer.valueOf(1), context.getData("b"));
        assertNull(context.getData("x"));
        assertNull(context.getData("2"));
    }

    private static final class TestableRenameArgNode extends RenameArgNode {
        private final Object cmpData;

        private TestableRenameArgNode(Object cmpData) {
            this.cmpData = cmpData;
        }

        @Override
        public <T> T getCmpData(Class<T> clazz) {
            @SuppressWarnings("unchecked")
            T value = (T) cmpData;
            return value;
        }
    }
}
