package team.carrypigeon.backend.chat.domain.controller;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import team.carrypigeon.backend.api.dao.cache.CPCache;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.chat.domain.controller.netty.service.email.send.CPServiceSendEmailVO;
import team.carrypigeon.backend.chat.domain.support.ChatDomainTestConfig;
import team.carrypigeon.backend.chat.domain.support.InMemoryDatabase;
import team.carrypigeon.backend.chat.domain.support.TestSession;
import team.carrypigeon.backend.chat.domain.support.dao.InMemoryEmailService;

/**
 * 集成测试：发送邮箱验证码相关 Controller。
 */
@SpringBootTest(classes = ChatDomainTestConfig.class)
public class ServiceEmailControllerFlowTests {

    @Autowired
    private FlowExecutor flowExecutor;

    @Autowired
    private InMemoryDatabase inMemoryDatabase;

    @Autowired
    private CPCache cache;

    @Autowired
    private InMemoryEmailService emailService;

    @AfterEach
    public void clearDatabase() {
        inMemoryDatabase.clearAll();
        emailService.clear();
    }

    @Test
    public void testServiceSendEmail_voInsertData() {
        CPFlowContext context = new CPFlowContext();
        context.setData("session", new TestSession());

        CPServiceSendEmailVO vo = new CPServiceSendEmailVO("test@example.com");
        Assertions.assertTrue(vo.insertData(context));
    }

    @Test
    public void testServiceSendEmail_flowExecute_shouldPass() {
        CPFlowContext context = new CPFlowContext();
        context.setData("session", new TestSession());

        CPServiceSendEmailVO vo = new CPServiceSendEmailVO("test@example.com");
        Assertions.assertTrue(vo.insertData(context));

        LiteflowResponse resp = flowExecutor.execute2Resp("/core/service/email/send", null, context);
        Assertions.assertNotNull(resp);
        Assertions.assertTrue(resp.isSuccess());

        String cacheCode = cache.get("test@example.com:code");
        Assertions.assertNotNull(cacheCode);
        Assertions.assertTrue(cacheCode.matches("^\\d{6}$"));

        Assertions.assertEquals("test@example.com", emailService.getLastTo());
        Assertions.assertNotNull(emailService.getLastSubject());
        Assertions.assertNotNull(emailService.getLastText());
        Assertions.assertTrue(emailService.getLastText().contains(cacheCode));
    }
}
