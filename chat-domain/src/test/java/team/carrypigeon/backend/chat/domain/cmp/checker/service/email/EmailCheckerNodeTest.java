package team.carrypigeon.backend.chat.domain.cmp.checker.service.email;

import com.yomahub.liteflow.slot.DefaultContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.chat.domain.ChatDomainNodeTestConfiguration;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyBasicConstants;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyExtraConstants;
import team.carrypigeon.backend.chat.domain.cmp.info.CheckResult;
import team.carrypigeon.backend.chat.domain.support.TestSession;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 邮箱相关 Checker Node 测试集合：
 *  - EmailCodeChecker
 *  - EmailExistsChecker
 *  - EmailValidChecker
 */
@SpringBootTest(classes = ChatDomainNodeTestConfiguration.class)
class EmailCheckerNodeTest {

    @Autowired
    private EmailCodeChecker emailCodeChecker;
    @Autowired
    private EmailExistsChecker emailExistsChecker;
    @Autowired
    private EmailValidChecker emailValidChecker;

    @Test
    void testEmailValidChecker_invalidEmailWritesError() {
        DefaultContext context = new DefaultContext();
        context.setData(CPNodeValueKeyExtraConstants.EMAIL, "not-an-email");
        CPReturnException ex = assertThrows(CPReturnException.class,
                () -> emailValidChecker.process(new TestSession(), context));
        assertNotNull(ex);
        CPResponse resp = context.getData(CPNodeValueKeyBasicConstants.RESPONSE);
        assertNotNull(resp);
    }

    @Test
    void testEmailExistsChecker_softModeCheckResult() throws Exception {
        DefaultContext context = new DefaultContext();
        context.setData(CPNodeValueKeyExtraConstants.EMAIL, "no_user@example.com");
        emailExistsChecker.process(new TestSession(), context);
        // soft 模式与 hard 模式区别通过 bind 配置，这里只验证不会抛异常
    }

    @Test
    void testEmailCodeChecker_missingParamsWritesError() {
        DefaultContext context = new DefaultContext();
        CPReturnException ex = assertThrows(CPReturnException.class,
                () -> emailCodeChecker.process(new TestSession(), context));
        assertNotNull(ex);
        CPResponse resp = context.getData(CPNodeValueKeyBasicConstants.RESPONSE);
        assertNotNull(resp);
    }
}