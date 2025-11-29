package team.carrypigeon.backend.chat.domain.controller;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import team.carrypigeon.backend.api.dao.cache.CPCache;
import team.carrypigeon.backend.chat.domain.attribute.CPChatDomainAttributes;
import team.carrypigeon.backend.chat.domain.controller.netty.file.CPFileDownloadTokenApplyVO;
import team.carrypigeon.backend.chat.domain.controller.netty.file.CPFileUploadTokenApplyVO;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyBasicConstants;
import team.carrypigeon.backend.chat.domain.support.ChatDomainTestConfig;
import team.carrypigeon.backend.chat.domain.support.InMemoryDatabase;
import team.carrypigeon.backend.chat.domain.support.TestSession;

/**
 * 集成测试：文件相关 Controller（token 申请）对应的 LiteFlow 链路。
 *
 * 覆盖：
 * - /core/file/upload/token/apply
 * - /core/file/download/token/apply
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = ChatDomainTestConfig.class)
public class FileControllerFlowTest {

    @Autowired
    private FlowExecutor flowExecutor;

    @Autowired
    private CPCache cache;

    @Autowired
    private InMemoryDatabase inMemoryDatabase;

    @After
    public void clearDatabase() {
        inMemoryDatabase.clearAll();
    }

    @Test
    public void testFileUploadTokenApply_success() {
        long uid = 1000L;

        TestSession session = new TestSession();
        session.setAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, uid);

        DefaultContext context = new DefaultContext();
        context.setData("session", session);

        CPFileUploadTokenApplyVO vo = new CPFileUploadTokenApplyVO();
        Assert.assertTrue(vo.insertData(context));

        LiteflowResponse resp = flowExecutor.execute2Resp("/core/file/upload/token/apply", null, context);
        Assert.assertTrue(resp.isSuccess());

        String token = context.getData(CPNodeValueKeyBasicConstants.FILE_TOKEN);
        Assert.assertNotNull(token);
    }

    @Test
    public void testFileDownloadTokenApply_success() {
        long uid = 1000L;
        String fileId = "file-123";

        TestSession session = new TestSession();
        session.setAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, uid);

        DefaultContext context = new DefaultContext();
        context.setData("session", session);

        CPFileDownloadTokenApplyVO vo = new CPFileDownloadTokenApplyVO(fileId);
        Assert.assertTrue(vo.insertData(context));

        LiteflowResponse resp = flowExecutor.execute2Resp("/core/file/download/token/apply", null, context);
        Assert.assertTrue(resp.isSuccess());

        String token = context.getData(CPNodeValueKeyBasicConstants.FILE_TOKEN);
        Assert.assertNotNull(token);
    }
}

