package team.carrypigeon.backend.chat.domain.cmp.biz.file;

import com.yomahub.liteflow.slot.DefaultContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import team.carrypigeon.backend.chat.domain.ChatDomainNodeTestConfiguration;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyBasicConstants;
import team.carrypigeon.backend.chat.domain.support.TestSession;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 文件相关 Node 测试集合（目前主要是 FileToken 相关）。
 */
@SpringBootTest(classes = ChatDomainNodeTestConfiguration.class)
class FileNodeTest {

    @Autowired
    private CPFileTokenCreatorNode fileTokenCreatorNode;

    @Test
    void testCPFileTokenCreatorNode_generatesToken() throws Exception {
        DefaultContext context = new DefaultContext();
        context.setData(CPNodeValueKeyBasicConstants.SESSION_ID, 1L);
        context.setData(CPNodeValueKeyBasicConstants.FILE_INFO_ID, "sha256-test");

        fileTokenCreatorNode.process(new TestSession(), context);
        String token = context.getData(CPNodeValueKeyBasicConstants.FILE_TOKEN);
        assertNotNull(token);
    }
}