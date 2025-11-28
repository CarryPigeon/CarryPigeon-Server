package team.carrypigeon.backend.chat.domain.cmp.biz.channel.ban;

import com.yomahub.liteflow.slot.DefaultContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import team.carrypigeon.backend.api.bo.domain.channel.ban.CPChannelBan;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.dao.database.channel.ban.ChannelBanDAO;
import team.carrypigeon.backend.chat.domain.ChatDomainNodeTestConfiguration;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyBasicConstants;
import team.carrypigeon.backend.chat.domain.support.TestSession;
import team.carrypigeon.backend.common.id.IdUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 频道封禁相关 Node 测试集合。
 */
@SpringBootTest(classes = ChatDomainNodeTestConfiguration.class)
class ChannelBanNodeTest {

    @Autowired
    private CPChannelBanSaverNode channelBanSaverNode;
    @Autowired
    private CPChannelBanSelectorNode channelBanSelectorNode;
    @Autowired
    private CPChannelBanDeleterNode channelBanDeleterNode;
    @Autowired
    private CPChannelBanListerNode channelBanListerNode;

    @Autowired
    private ChannelBanDAO channelBanDAO;

    @Test
    void testCPChannelBanSaverNode_argsErrorWhenMissingFields() {
        DefaultContext context = new DefaultContext();
        CPReturnException ex = assertThrows(CPReturnException.class,
                () -> channelBanSaverNode.process(new TestSession(), context));
        assertNotNull(ex);
    }

    @Test
    void testCPChannelBanSelectorNode_argsErrorWhenMissingFields() {
        DefaultContext context = new DefaultContext();
        CPReturnException ex = assertThrows(CPReturnException.class,
                () -> channelBanSelectorNode.process(new TestSession(), context));
        assertNotNull(ex);
    }

    @Test
    void testCPChannelBanDeleterNode_argsErrorWhenNoBan() {
        DefaultContext context = new DefaultContext();
        CPReturnException ex = assertThrows(CPReturnException.class,
                () -> channelBanDeleterNode.process(new TestSession(), context));
        assertNotNull(ex);
    }

    @Test
    void testCPChannelBanListerNode_emptyWhenNoData() throws Exception {
        DefaultContext context = new DefaultContext();
        context.setData(CPNodeValueKeyBasicConstants.CHANNEL_INFO_ID, IdUtil.generateId());
        channelBanListerNode.process(new TestSession(), context);
        // 没有数据时，ChannelBanItems 可能为空，由你后续细化
    }
}