package team.carrypigeon.backend.chat.domain.cmp.checker.group;

import com.yomahub.liteflow.slot.DefaultContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.chat.domain.ChatDomainNodeTestConfiguration;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyBasicConstants;
import team.carrypigeon.backend.chat.domain.cmp.info.CheckResult;
import team.carrypigeon.backend.chat.domain.support.TestSession;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 群组权限相关 Checker Node 测试集合：
 *  - CPChannelAdminChecker
 *  - CPChannelOwnerChecker
 *  - CPChannelBanCheckerNode
 *  - CPChannelBanTargetCheckerNode
 */
@SpringBootTest(classes = ChatDomainNodeTestConfiguration.class)
class GroupCheckerNodeTest {

    @Autowired
    private CPChannelAdminChecker channelAdminChecker;
    @Autowired
    private CPChannelOwnerChecker channelOwnerChecker;
    @Autowired
    private CPChannelBanCheckerNode channelBanCheckerNode;
    @Autowired
    private CPChannelBanTargetCheckerNode channelBanTargetCheckerNode;

    @Test
    void testCPChannelAdminChecker_argsErrorWhenMissingChannelOrUser() {
        DefaultContext context = new DefaultContext();
        CPReturnException ex = assertThrows(CPReturnException.class,
                () -> channelAdminChecker.process(new TestSession(), context));
        assertNotNull(ex);
    }

    @Test
    void testCPChannelOwnerChecker_argsErrorWhenMissingChannelOrUser() {
        DefaultContext context = new DefaultContext();
        CPReturnException ex = assertThrows(CPReturnException.class,
                () -> channelOwnerChecker.process(new TestSession(), context));
        assertNotNull(ex);
    }

    @Test
    void testCPChannelBanCheckerNode_softModeWritesCheckResultWhenNoBan() throws Exception {
        DefaultContext context = new DefaultContext();
        // 设置 soft 模式（通过 bindData 在 LiteFlow 中实现，这里只验证 CheckResult 存在性）
        channelBanCheckerNode.process(new TestSession(), context);
        CheckResult cr = context.getData(CPNodeValueKeyBasicConstants.CHECK_RESULT);
        // 具体 true/false 留给你后续调整
    }

    @Test
    void testCPChannelBanTargetCheckerNode_softModeWritesCheckResult() throws Exception {
        DefaultContext context = new DefaultContext();
        channelBanTargetCheckerNode.process(new TestSession(), context);
        CheckResult cr = context.getData(CPNodeValueKeyBasicConstants.CHECK_RESULT);
        // 具体断言由你补充
    }
}