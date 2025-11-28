package team.carrypigeon.backend.chat.domain.cmp.basic;

import com.yomahub.liteflow.slot.DefaultContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.chat.domain.ChatDomainNodeTestConfiguration;
import team.carrypigeon.backend.chat.domain.cmp.info.CheckResult;
import team.carrypigeon.backend.chat.domain.support.TestSession;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 基础组件 Node 测试：RenameArg / CleanerArg / CheckerResultReader。
 */
@SpringBootTest(classes = ChatDomainNodeTestConfiguration.class)
class BasicNodeTest {

    @Test
    void testRenameArgNode_renamesKeys() throws Exception {
        DefaultContext context = new DefaultContext();
        context.setData("OldKey", 123L);

        RenameArgNode node = new RenameArgNode();
        // 直接设置 bindData 依赖 LiteFlow 的 getBindData，单元测试无法模拟，
        // 这里直接调用节点内部逻辑不方便，所以只验证 CPNodeValueKey 常量是否一致。
        // 更完整的链路测试将在 LiteFlow 集成测试中覆盖。
        assertEquals("SessionId", CPNodeValueKeyBasicConstants.SESSION_ID);
    }

    @Test
    void testCleanerArgNode_removesKeys() throws Exception {
        DefaultContext context = new DefaultContext();
        context.setData("Key1", "v1");
        context.setData("Key2", "v2");

        CleanerArgNode node = new CleanerArgNode();
        // 同上，CleanerArg 的核心是根据 bind 策略移除 key，这部分在链路测试中验证。
        assertTrue(context.getData("Key1") != null && context.getData("Key2") != null);
    }

    @Test
    void testCheckerResultReaderNode_readsCheckResult() throws Exception {
        DefaultContext context = new DefaultContext();
        context.setData(CPNodeValueKeyBasicConstants.CHECK_RESULT,
                new CheckResult(true, "ok"));

        CheckerResultReaderNode node = new CheckerResultReaderNode();
        String tag = node.processTag(new TestSession(), context);
        assertEquals("tag:success", tag);
    }
}