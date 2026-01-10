package team.carrypigeon.backend.chat.domain.cmp.biz.message;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.message.CPMessageData;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelMemberKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeCommonKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeMessageKeys;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyExtraConstants;
import team.carrypigeon.backend.chat.domain.cmp.info.CheckResult;
import team.carrypigeon.backend.chat.domain.service.message.CPMessageParserService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CPMessageParseNodeTests {

    @Test
    void process_domainOrDataNull_shouldThrowArgsError() {
        CPMessageParserService parserService = mock(CPMessageParserService.class);
        CPMessageParseNode node = new TestableCPMessageParseNode(parserService, null);

        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeMessageKeys.MESSAGE_INFO_DOMAIN, "Core:Text");

        assertThrows(CPReturnException.class, () -> node.process(null, context));
        CPResponse response = context.getData(CPNodeCommonKeys.RESPONSE);
        assertNotNull(response);
        assertEquals(100, response.getCode());
        assertEquals("error args", response.getData().get("msg").asText());
    }

    @Test
    void process_parserReturnsNull_hard_shouldThrowBusinessError() {
        CPMessageParserService parserService = mock(CPMessageParserService.class);
        when(parserService.parse(anyString(), any())).thenReturn(null);
        CPMessageParseNode node = new TestableCPMessageParseNode(parserService, null);

        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeMessageKeys.MESSAGE_INFO_DOMAIN, "Core:Text");
        context.setData(CPNodeMessageKeys.MESSAGE_INFO_DATA, new ObjectMapper().createObjectNode());

        assertThrows(CPReturnException.class, () -> node.process(null, context));
        CPResponse response = context.getData(CPNodeCommonKeys.RESPONSE);
        assertNotNull(response);
        assertEquals(100, response.getCode());
        assertEquals("message type error", response.getData().get("msg").asText());
    }

    @Test
    void process_parserReturnsNull_soft_shouldWriteCheckResultAndReturn() {
        CPMessageParserService parserService = mock(CPMessageParserService.class);
        when(parserService.parse(anyString(), any())).thenReturn(null);
        CPMessageParseNode node = new TestableCPMessageParseNode(parserService, "soft");

        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeMessageKeys.MESSAGE_INFO_DOMAIN, "Core:Text");
        context.setData(CPNodeMessageKeys.MESSAGE_INFO_DATA, new ObjectMapper().createObjectNode());

        assertDoesNotThrow(() -> node.process(null, context));

        assertNull(context.getData(CPNodeCommonKeys.RESPONSE));
        CheckResult result = context.getData(CPNodeCommonKeys.CHECK_RESULT);
        assertNotNull(result);
        assertFalse(result.state());
        assertEquals("message type error", result.msg());
    }

    @Test
    void process_success_soft_shouldWriteMessageDataAndCheckResult() throws Exception {
        CPMessageParserService parserService = mock(CPMessageParserService.class);
        CPMessageData messageData = mock(CPMessageData.class);
        JsonNode data = new ObjectMapper().readTree("{\"x\":1}");
        when(parserService.parse(eq("Core:Text"), eq(data))).thenReturn(messageData);
        CPMessageParseNode node = new TestableCPMessageParseNode(parserService, "soft");

        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeMessageKeys.MESSAGE_INFO_DOMAIN, "Core:Text");
        context.setData(CPNodeMessageKeys.MESSAGE_INFO_DATA, data);
        context.setData(CPNodeChannelKeys.CHANNEL_INFO_ID, 1L);
        context.setData(CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO_UID, 2L);

        node.process(null, context);

        assertSame(messageData, context.getData(CPNodeValueKeyExtraConstants.MESSAGE_DATA));
        CheckResult result = context.getData(CPNodeCommonKeys.CHECK_RESULT);
        assertNotNull(result);
        assertTrue(result.state());
        assertNull(result.msg());
    }

    private static final class TestableCPMessageParseNode extends CPMessageParseNode {
        private final String type;

        private TestableCPMessageParseNode(CPMessageParserService cpMessageParserService, String type) {
            super(cpMessageParserService);
            this.type = type;
        }

        @Override
        public <T> T getBindData(String key, Class<T> clazz) {
            if ("type".equals(key) && clazz == String.class) {
                return clazz.cast(type);
            }
            return null;
        }
    }
}

