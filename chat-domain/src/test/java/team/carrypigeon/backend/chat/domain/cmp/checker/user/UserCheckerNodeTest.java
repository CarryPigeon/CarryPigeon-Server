package team.carrypigeon.backend.chat.domain.cmp.checker.user;

import com.yomahub.liteflow.slot.DefaultContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.chat.domain.ChatDomainNodeTestConfiguration;
import team.carrypigeon.backend.chat.domain.support.TestSession;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用户登录 Checker Node 测试集合。
 */
@SpringBootTest(classes = ChatDomainNodeTestConfiguration.class)
class UserCheckerNodeTest {

    @Autowired
    private UserLoginCheckerNode userLoginCheckerNode;

    @Test
    void testUserLoginCheckerNode_notLoggedInThrows() {
        DefaultContext context = new DefaultContext();
        TestSession session = new TestSession();
        CPReturnException ex = assertThrows(CPReturnException.class,
                () -> userLoginCheckerNode.process(session, context));
        assertNotNull(ex);
    }
}