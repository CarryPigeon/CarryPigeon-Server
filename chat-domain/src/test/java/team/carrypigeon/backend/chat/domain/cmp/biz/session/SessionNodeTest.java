package team.carrypigeon.backend.chat.domain.cmp.biz.session;

import com.yomahub.liteflow.slot.DefaultContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import team.carrypigeon.backend.chat.domain.ChatDomainNodeTestConfiguration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 会话注册 Node 测试集合。
 */
@SpringBootTest(classes = ChatDomainNodeTestConfiguration.class)
class SessionNodeTest {

    @Autowired
    private CPSessionRegisterNode sessionRegisterNode;

    @Test
    void testCPSessionRegisterNode_beanLoaded() {
        assertNotNull(sessionRegisterNode);
    }
}