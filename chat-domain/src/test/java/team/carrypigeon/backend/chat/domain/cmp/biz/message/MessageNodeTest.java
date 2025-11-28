package team.carrypigeon.backend.chat.domain.cmp.biz.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yomahub.liteflow.slot.DefaultContext;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMemberAuthorityEnum;
import team.carrypigeon.backend.api.bo.domain.message.CPMessage;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.api.chat.domain.message.CPMessageData;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.dao.database.channel.member.ChannelMemberDao;
import team.carrypigeon.backend.api.dao.database.message.ChannelMessageDao;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyBasicConstants;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyExtraConstants;
import team.carrypigeon.backend.chat.domain.cmp.info.CheckResult;
import team.carrypigeon.backend.chat.domain.service.message.CPMessageParserService;
import team.carrypigeon.backend.common.time.TimeUtil;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/**
 * 娑堟伅鐩稿叧 Node 鐨勫崟鍏冩祴璇曢泦鍚堛€? */
class MessageNodeTest {

    private static class TestSession implements CPSession {
        private final Map<String, Object> attrs = new HashMap<>();

        @Override
        public void write(String msg, boolean encrypted) {
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getAttributeValue(String key, Class<T> type) {
            Object value = attrs.get(key);
            return value == null ? null : (T) value;
        }

        @Override
        public void setAttributeValue(String key, Object value) {
            attrs.put(key, value);
        }

        @Override
        public void close() {
        }
    }

    @Test
    void testCPMessageParseNode_hardMode_success() throws Exception {
        DefaultContext context = new DefaultContext();
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode json = mapper.createObjectNode().put("text", "hello");

        context.setData(CPNodeValueKeyBasicConstants.MESSAGE_INFO_DOMAIN, "Core:Text");
        context.setData(CPNodeValueKeyBasicConstants.MESSAGE_INFO_DATA, json);

        CPMessageData parsed = new CPMessageData()
                .setDomain("Core:Text")
                .setData(json)
                .setSContent("hello");

        CPMessageParserService parser = mock(CPMessageParserService.class);
        when(parser.parse("Core:Text", json)).thenReturn(parsed);

        CPMessageParseNode node = new CPMessageParseNode(parser);
        node.process(new TestSession(), context);

        CPMessageData result = context.getData(CPNodeValueKeyExtraConstants.MESSAGE_DATA);
        assertNotNull(result);
        assertEquals("Core:Text", result.getDomain());
        assertEquals("hello", result.getSContent());
        CheckResult cr = context.getData(CPNodeValueKeyBasicConstants.CHECK_RESULT);
        assertNull(cr); // hard 妯″紡涓嶄細鍐?CheckResult
    }

    @Test
    void testCPMessageBuilderNode_buildsMessageFromData() throws Exception {
        DefaultContext context = new DefaultContext();
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode json = mapper.createObjectNode().put("text", "hello");

        CPMessageData data = new CPMessageData()
                .setDomain("Core:Text")
                .setData(json)
                .setSContent("hello");
        context.setData(CPNodeValueKeyExtraConstants.MESSAGE_DATA, data);
        context.setData(CPNodeValueKeyBasicConstants.CHANNEL_INFO_ID, 1L);
        context.setData(CPNodeValueKeyBasicConstants.CHANNEL_MEMBER_INFO_UID, 2L);

        CPMessageBuilderNode node = new CPMessageBuilderNode();
        node.process(new TestSession(), context);

        CPMessage msg = context.getData(CPNodeValueKeyBasicConstants.MESSAGE_INFO);
        assertNotNull(msg);
        assertEquals(1L, msg.getCid());
        assertEquals(2L, msg.getUid());
        assertEquals("Core:Text", msg.getDomain());
        assertEquals(json, msg.getData());
    }

    @Test
    void testCPMessageSaverNode_savesMessage() throws Exception {
        DefaultContext context = new DefaultContext();
        CPMessage message = new CPMessage().setId(1L).setCid(1L).setUid(2L);
        context.setData(CPNodeValueKeyBasicConstants.MESSAGE_INFO, message);

        ChannelMessageDao dao = mock(ChannelMessageDao.class);
        when(dao.save(message)).thenReturn(true);

        CPMessageSaverNode node = new CPMessageSaverNode(dao);
        node.process(new TestSession(), context);

        verify(dao, times(1)).save(message);
        assertNull(context.getData(CPNodeValueKeyBasicConstants.RESPONSE));
    }

    @Test
    void testCPMessageListerNode_listsMessages() throws Exception {
        DefaultContext context = new DefaultContext();
        context.setData(CPNodeValueKeyBasicConstants.CHANNEL_INFO_ID, 1L);
        context.setData(CPNodeValueKeyBasicConstants.MESSAGE_LIST_START_TIME, TimeUtil.getCurrentTime());
        context.setData(CPNodeValueKeyBasicConstants.MESSAGE_LIST_COUNT, 10);

        ChannelMessageDao dao = mock(ChannelMessageDao.class);
        when(dao.getBefore(anyLong(), any(LocalDateTime.class), anyInt()))
                .thenReturn(new CPMessage[]{new CPMessage().setId(1L)});

        CPMessageListerNode node = new CPMessageListerNode(dao);
        node.process(new TestSession(), context);

        CPMessage[] messages = context.getData(CPNodeValueKeyBasicConstants.MESSAGE_LIST);
        assertNotNull(messages);
        assertEquals(1, messages.length);
    }

    @Test
    void testCPMessageSelectorNode_selectsMessage() throws Exception {
        DefaultContext context = new DefaultContext();
        context.setData(CPNodeValueKeyBasicConstants.MESSAGE_INFO_ID, 1L);

        CPMessage message = new CPMessage().setId(1L).setCid(10L).setUid(20L);
        ChannelMessageDao dao = mock(ChannelMessageDao.class);
        when(dao.getById(1L)).thenReturn(message);

        CPMessageSelectorNode node = new CPMessageSelectorNode(dao);
        node.process(new TestSession(), context);

        CPMessage loaded = context.getData(CPNodeValueKeyBasicConstants.MESSAGE_INFO);
        assertNotNull(loaded);
        assertEquals(10L, context.getData(CPNodeValueKeyBasicConstants.CHANNEL_INFO_ID));
    }

    @Test
    void testCPMessageUnreadCounterNode_countsUnread() throws Exception {
        DefaultContext context = new DefaultContext();
        context.setData(CPNodeValueKeyBasicConstants.CHANNEL_INFO_ID, 1L);
        context.setData(CPNodeValueKeyBasicConstants.MESSAGE_UNREAD_START_TIME, TimeUtil.getCurrentTime());
        context.setData(CPNodeValueKeyBasicConstants.SESSION_ID, 2L);

        ChannelMessageDao dao = mock(ChannelMessageDao.class);
        when(dao.getAfterCount(anyLong(), anyLong(), any(LocalDateTime.class)))
                .thenReturn(5);

        CPMessageUnreadCounterNode node = new CPMessageUnreadCounterNode(dao);
        node.process(new TestSession(), context);

        Long count = context.getData(CPNodeValueKeyBasicConstants.MESSAGE_UNREAD_COUNT);
        assertEquals(5L, count);
    }

    @Test
    void testCPMessageDeletePermissionCheckerNode_ownerCanDelete() throws Exception {
        DefaultContext context = new DefaultContext();
        CPMessage message = new CPMessage()
                .setId(1L)
                .setCid(10L)
                .setUid(100L)
                .setSendTime(LocalDateTime.now());
        context.setData(CPNodeValueKeyBasicConstants.MESSAGE_INFO, message);
        context.setData(CPNodeValueKeyBasicConstants.SESSION_ID, 100L);

        ChannelMemberDao dao = mock(ChannelMemberDao.class);
        CPMessageDeletePermissionCheckerNode node = new CPMessageDeletePermissionCheckerNode(dao);
        node.process(new TestSession(), context);
        // 鏃犲紓甯稿嵆瑙嗕负閫氳繃
    }

    @Test
    void testCPMessageDeleterNode_deletesMessage() throws Exception {
        DefaultContext context = new DefaultContext();
        CPMessage message = new CPMessage().setId(1L).setCid(10L).setUid(20L);
        context.setData(CPNodeValueKeyBasicConstants.MESSAGE_INFO, message);

        ChannelMessageDao dao = mock(ChannelMessageDao.class);
        when(dao.delete(message)).thenReturn(true);

        CPMessageDeleterNode node = new CPMessageDeleterNode(dao);
        node.process(new TestSession(), context);

        verify(dao, times(1)).delete(message);
    }
}
