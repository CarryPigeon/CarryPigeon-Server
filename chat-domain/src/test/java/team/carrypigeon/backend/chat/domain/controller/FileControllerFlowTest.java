package team.carrypigeon.backend.chat.domain.controller;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.dao.cache.CPCache;
import team.carrypigeon.backend.chat.domain.attribute.CPChatDomainAttributes;
import team.carrypigeon.backend.chat.domain.controller.netty.file.CPFileDownloadTokenApplyVO;
import team.carrypigeon.backend.chat.domain.controller.netty.file.CPFileUploadTokenApplyVO;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeFileKeys;
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
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = ChatDomainTestConfig.class)
public class FileControllerFlowTest {

    @Autowired
    private FlowExecutor flowExecutor;

    @Autowired
    private CPCache cache;

    @Autowired
    private InMemoryDatabase inMemoryDatabase;

    @AfterEach
    public void clearDatabase() {
        inMemoryDatabase.clearAll();
    }

    @Test
    public void testFileUploadTokenApply_success() {
        long uid = 1000L;

        TestSession session = new TestSession();
        session.setAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, uid);

        CPFlowContext context = new CPFlowContext();
        context.setData("session", session);

        CPFileUploadTokenApplyVO vo = new CPFileUploadTokenApplyVO();
        Assertions.assertTrue(vo.insertData(context));

        LiteflowResponse resp = flowExecutor.execute2Resp("/core/file/upload/token/apply", null, context);
        Assertions.assertTrue(resp.isSuccess());

        String token = context.getData(CPNodeFileKeys.FILE_TOKEN);
        Assertions.assertNotNull(token);
    }

    @Test
    public void testFileDownloadTokenApply_success() {
        long uid = 1000L;
        String fileId = "file-123";

        TestSession session = new TestSession();
        session.setAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, uid);

        CPFlowContext context = new CPFlowContext();
        context.setData("session", session);

        CPFileDownloadTokenApplyVO vo = new CPFileDownloadTokenApplyVO(fileId);
        Assertions.assertTrue(vo.insertData(context));

        LiteflowResponse resp = flowExecutor.execute2Resp("/core/file/download/token/apply", null, context);
        Assertions.assertTrue(resp.isSuccess());

        String token = context.getData(CPNodeFileKeys.FILE_TOKEN);
        Assertions.assertNotNull(token);
    }
}
