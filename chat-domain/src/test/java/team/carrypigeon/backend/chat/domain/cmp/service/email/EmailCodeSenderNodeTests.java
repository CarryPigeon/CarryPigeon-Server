package team.carrypigeon.backend.chat.domain.cmp.service.email;

import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.dao.cache.CPCache;
import team.carrypigeon.backend.api.service.email.CPEmailService;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeCommonKeys;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyExtraConstants;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EmailCodeSenderNodeTests {

    @Test
    void process_mailDisabled_shouldThrowAndSetError() throws Exception {
        CPCache cache = mock(CPCache.class);
        CPEmailService emailService = mock(CPEmailService.class);
        EmailCodeSenderNode node = new EmailCodeSenderNode(cache, emailService);
        setField(node, "mailEnabled", false);

        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeValueKeyExtraConstants.EMAIL, "a@b.com");

        assertThrows(CPReturnException.class, () -> node.process(null, context));
        CPResponse response = context.getData(CPNodeCommonKeys.RESPONSE);
        assertNotNull(response);
        assertEquals("email service disabled", response.getData().get("msg").asText());
    }

    @Test
    void process_success_shouldWriteCacheAndSendEmail() throws Exception {
        CPCache cache = mock(CPCache.class);
        CPEmailService emailService = mock(CPEmailService.class);
        EmailCodeSenderNode node = new EmailCodeSenderNode(cache, emailService);
        setField(node, "mailEnabled", true);
        setField(node, "codeExpireSeconds", 30);
        setField(node, "subject", "S");

        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeValueKeyExtraConstants.EMAIL, "a@b.com");

        node.process(null, context);

        var keyCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        var codeCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        var expireCaptor = org.mockito.ArgumentCaptor.forClass(Integer.class);
        verify(cache).set(keyCaptor.capture(), codeCaptor.capture(), expireCaptor.capture());
        assertEquals("a@b.com:code", keyCaptor.getValue());
        assertTrue(codeCaptor.getValue().matches("\\d{6}"));
        assertEquals(30, expireCaptor.getValue());

        var contentCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(emailService).sendEmail(eq("a@b.com"), eq("S"), contentCaptor.capture());
        assertTrue(contentCaptor.getValue().contains(codeCaptor.getValue()));
        assertTrue(contentCaptor.getValue().contains("1 分钟内有效"));
    }

    @Test
    void process_configuredExpireInvalid_shouldDefault300() throws Exception {
        CPCache cache = mock(CPCache.class);
        CPEmailService emailService = mock(CPEmailService.class);
        EmailCodeSenderNode node = new EmailCodeSenderNode(cache, emailService);
        setField(node, "mailEnabled", true);
        setField(node, "codeExpireSeconds", 0);

        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeValueKeyExtraConstants.EMAIL, "a@b.com");

        node.process(null, context);

        verify(cache).set(eq("a@b.com:code"), anyString(), eq(300));
    }

    @Test
    void process_sendEmailThrows_shouldRollbackCacheAndReturnServerError() throws Exception {
        CPCache cache = mock(CPCache.class);
        CPEmailService emailService = mock(CPEmailService.class);
        EmailCodeSenderNode node = new EmailCodeSenderNode(cache, emailService);
        setField(node, "mailEnabled", true);
        setField(node, "subject", "S");

        doThrow(new RuntimeException("send fail")).when(emailService).sendEmail(anyString(), anyString(), anyString());
        doThrow(new RuntimeException("delete fail")).when(cache).delete(anyString());

        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeValueKeyExtraConstants.EMAIL, "a@b.com");

        assertThrows(CPReturnException.class, () -> node.process(null, context));
        verify(cache).set(eq("a@b.com:code"), anyString(), anyInt());
        verify(cache).delete(eq("a@b.com:code"));

        CPResponse response = context.getData(CPNodeCommonKeys.RESPONSE);
        assertNotNull(response);
        assertEquals(500, response.getCode());
        assertEquals("send email error", response.getData().get("msg").asText());
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }
}
