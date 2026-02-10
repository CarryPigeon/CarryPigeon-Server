package team.carrypigeon.backend.chat.domain.cmp.biz.message;

import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.message.CPMessageData;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelMemberKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeMessageKeys;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyExtraConstants;
import team.carrypigeon.backend.api.chat.domain.flow.CheckResult;
import team.carrypigeon.backend.chat.domain.service.catalog.ApiDomainContractService;
import team.carrypigeon.backend.chat.domain.service.message.CPMessageParserService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CPMessageParseNodeTests {

    @Test
    void process_domainOrDataNull_shouldThrowArgsError() {
        CPMessageParserService parserService = mock(CPMessageParserService.class);
        ApiDomainContractService contractService = mock(ApiDomainContractService.class);
        CPMessageParseNode node = new TestableCPMessageParseNode(parserService, contractService, null);

        CPFlowContext context = new CPFlowContext();
        context.set(CPNodeMessageKeys.MESSAGE_INFO_DOMAIN, "Core:Text");

        CPProblemException ex = assertThrows(CPProblemException.class, () -> node.process(null, context));
        assertEquals(422, ex.getProblem().status());
        assertEquals("validation_failed", ex.getProblem().reason().code());
    }

    @Test
    void process_parserReturnsNull_hard_shouldThrowBusinessError() {
        CPMessageParserService parserService = mock(CPMessageParserService.class);
        when(parserService.supports(anyString())).thenReturn(true);
        when(parserService.parse(anyString(), any())).thenReturn(null);
        ApiDomainContractService contractService = mock(ApiDomainContractService.class);
        CPMessageParseNode node = new TestableCPMessageParseNode(parserService, contractService, null);

        CPFlowContext context = new CPFlowContext();
        context.set(CPNodeMessageKeys.MESSAGE_INFO_DOMAIN, "Core:Text");
        context.set(CPNodeMessageKeys.MESSAGE_INFO_DATA, new ObjectMapper().createObjectNode());

        CPProblemException ex = assertThrows(CPProblemException.class, () -> node.process(null, context));
        assertEquals(422, ex.getProblem().status());
        assertEquals("schema_invalid", ex.getProblem().reason().code());
    }

    @Test
    void process_parserReturnsNull_soft_shouldWriteCheckResultAndReturn() {
        CPMessageParserService parserService = mock(CPMessageParserService.class);
        when(parserService.supports(anyString())).thenReturn(true);
        when(parserService.parse(anyString(), any())).thenReturn(null);
        ApiDomainContractService contractService = mock(ApiDomainContractService.class);
        CPMessageParseNode node = new TestableCPMessageParseNode(parserService, contractService, "soft");

        CPFlowContext context = new CPFlowContext();
        context.set(CPNodeMessageKeys.MESSAGE_INFO_DOMAIN, "Core:Text");
        context.set(CPNodeMessageKeys.MESSAGE_INFO_DATA, new ObjectMapper().createObjectNode());

        assertDoesNotThrow(() -> node.process(null, context));

        assertNull(context.get(CPFlowKeys.RESPONSE));
        CheckResult result = context.get(CPFlowKeys.CHECK_RESULT);
        assertNotNull(result);
        assertFalse(result.state());
        assertEquals("message type error", result.msg());
    }

    @Test
    void process_success_soft_shouldWriteMessageDataAndCheckResult() throws Exception {
        CPMessageParserService parserService = mock(CPMessageParserService.class);
        when(parserService.supports(anyString())).thenReturn(true);
        ApiDomainContractService contractService = mock(ApiDomainContractService.class);
        CPMessageData messageData = mock(CPMessageData.class);
        JsonNode data = new ObjectMapper().readTree("{\"x\":1}");
        when(parserService.parse(eq("Core:Text"), eq(data))).thenReturn(messageData);
        CPMessageParseNode node = new TestableCPMessageParseNode(parserService, contractService, "soft");

        CPFlowContext context = new CPFlowContext();
        context.set(CPNodeMessageKeys.MESSAGE_INFO_DOMAIN, "Core:Text");
        context.set(CPNodeMessageKeys.MESSAGE_INFO_DATA, data);
        context.set(CPNodeChannelKeys.CHANNEL_INFO_ID, 1L);
        context.set(CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO_UID, 2L);

        node.process(null, context);

        assertSame(messageData, context.get(CPNodeValueKeyExtraConstants.MESSAGE_DATA));
        CheckResult result = context.get(CPFlowKeys.CHECK_RESULT);
        assertNotNull(result);
        assertTrue(result.state());
        assertNull(result.msg());
    }

    private static final class TestableCPMessageParseNode extends CPMessageParseNode {
        private final String type;

        private TestableCPMessageParseNode(CPMessageParserService cpMessageParserService,
                                           ApiDomainContractService contractService,
                                           String type) {
            super(cpMessageParserService, contractService);
            this.type = type;
        }

        /**
         * 测试辅助方法。
         *
         * @param key 测试输入参数
         * @param clazz 测试输入参数
         * @return 测试辅助方法返回结果
         */
        @Override
        public <T> T getBindData(String key, Class<T> clazz) {
            if ("type".equals(key) && clazz == String.class) {
                return clazz.cast(type);
            }
            return null;
        }
    }
}
