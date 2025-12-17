package team.carrypigeon.backend.chat.domain.controller;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.chat.domain.controller.netty.service.email.send.CPServiceSendEmailVO;
import team.carrypigeon.backend.chat.domain.support.ChatDomainTestConfig;
import team.carrypigeon.backend.chat.domain.support.InMemoryDatabase;
import team.carrypigeon.backend.chat.domain.support.TestSession;

/**
 * 集成测试：发送邮箱验证码相关 Controller。
 *
 * 说明：当前仓库中未提供 /core/service/email/send 的 LiteFlow 规则配置，
 * 因此这里只验证 VO.insertData 与 LiteFlow 调用过程是否能被正确触发。
 * 如果后续补充了对应的链路配置，可以在此基础上增加更详细的断言。
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = ChatDomainTestConfig.class)
public class ServiceEmailControllerFlowTest {

    @Autowired
    private FlowExecutor flowExecutor;

    @Autowired
    private InMemoryDatabase inMemoryDatabase;

    @After
    public void clearDatabase() {
        inMemoryDatabase.clearAll();
    }

    @Test
    public void testServiceSendEmail_voInsertData() {
        CPFlowContext context = new CPFlowContext();
        context.setData("session", new TestSession());

        CPServiceSendEmailVO vo = new CPServiceSendEmailVO("test@example.com");
        Assert.assertTrue(vo.insertData(context));
    }

    @Test
    public void testServiceSendEmail_flowExecute_shouldPass() {
        CPFlowContext context = new CPFlowContext();
        context.setData("session", new TestSession());

        CPServiceSendEmailVO vo = new CPServiceSendEmailVO("test@example.com");
        Assert.assertTrue(vo.insertData(context));

        LiteflowResponse resp = flowExecutor.execute2Resp("/core/service/email/send", null, context);
        Assert.assertNotNull(resp);
        Assert.assertTrue(resp.isSuccess());
    }
}
