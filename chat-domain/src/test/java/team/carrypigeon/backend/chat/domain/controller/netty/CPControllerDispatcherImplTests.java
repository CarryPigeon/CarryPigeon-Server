package team.carrypigeon.backend.chat.domain.controller.netty;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yomahub.liteflow.flow.LiteflowResponse;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import team.carrypigeon.backend.api.bo.connection.CPConnectionAttributes;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerResult;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerVO;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.chat.domain.attribute.CPChatDomainAttributes;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeCommonKeys;
import team.carrypigeon.backend.chat.domain.flow.FlowTxExecutor;
import team.carrypigeon.backend.chat.domain.service.session.CPSessionCenterService;
import team.carrypigeon.backend.chat.domain.support.TestSession;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CPControllerDispatcherImplTests {

    @CPControllerTag(path = "/core/test/ok", voClazz = OkVO.class, resultClazz = OkResult.class)
    public static class OkController {
    }

    public static class OkVO implements CPControllerVO {
        @Override
        public boolean insertData(CPFlowContext context) {
            context.setData("ok", true);
            return true;
        }
    }

    public static class BadVO implements CPControllerVO {
        @Override
        public boolean insertData(CPFlowContext context) {
            return false;
        }
    }

    @CPControllerTag(path = "/core/test/bad-vo", voClazz = BadVO.class, resultClazz = OkResult.class)
    public static class BadVoController {
    }

    public static class OkResult implements CPControllerResult {
        @Override
        public void process(CPSession session, CPFlowContext context, ObjectMapper objectMapper) {
            context.setData(CPNodeCommonKeys.RESPONSE, CPResponse.success().setTextData("ok"));
        }
    }

    @CPControllerTag(path = "/core/test/no-ctor", voClazz = OkVO.class, resultClazz = NoDefaultCtorResult.class)
    public static class NoCtorController {
    }

    public static class NoDefaultCtorResult implements CPControllerResult {
        public NoDefaultCtorResult(String ignored) {
        }

        @Override
        public void process(CPSession session, CPFlowContext context, ObjectMapper objectMapper) {
        }
    }

    @Test
    void process_routeNotFound_shouldReturn404() {
        CPControllerDispatcherImpl dispatcher = newDispatcherWithBeans(Map.of());
        CPResponse response = dispatcher.process("{\"id\":1,\"route\":\"/core/unknown\",\"data\":{}}", new TestSession());
        assertEquals(404, response.getCode());
    }

    @Test
    void process_insertDataFalse_shouldReturnError100() {
        CPControllerDispatcherImpl dispatcher = newDispatcherWithBeans(Map.of("bad", new BadVoController()));
        CPResponse response = dispatcher.process("{\"id\":1,\"route\":\"/core/test/bad-vo\",\"data\":{}}", new TestSession());
        assertEquals(100, response.getCode());
        assertEquals("invalid request args", response.getData().get("msg").asText());
    }

    @Test
    void process_resultNoDefaultConstructor_shouldReturnServerErrorWithId() {
        CPControllerDispatcherImpl dispatcher = newDispatcherWithBeans(Map.of("noCtor", new NoCtorController()));
        CPResponse response = dispatcher.process("{\"id\":77,\"route\":\"/core/test/no-ctor\",\"data\":{}}", new TestSession());
        assertEquals(500, response.getCode());
        assertEquals(77, response.getId());
    }

    @Test
    void process_success_shouldReturnResponseWithIdAndRunChannelInactive() {
        CPSessionCenterService sessionCenterService = mock(CPSessionCenterService.class);
        FlowTxExecutor flowTxExecutor = mock(FlowTxExecutor.class);
        ObjectMapper mapper = new ObjectMapper();
        ApplicationContext applicationContext = mock(ApplicationContext.class);

        when(applicationContext.getBeansWithAnnotation(CPControllerTag.class))
                .thenReturn(Map.of("ok", new OkController()));

        LiteflowResponse liteflowResponse = mock(LiteflowResponse.class);
        when(liteflowResponse.isSuccess()).thenReturn(true);
        java.util.concurrent.atomic.AtomicReference<CPFlowContext> ctxRef = new java.util.concurrent.atomic.AtomicReference<>();
        when(liteflowResponse.getContextBean(eq(CPFlowContext.class))).thenAnswer(invocation -> ctxRef.get());
        when(flowTxExecutor.executeWithTx(eq("/core/test/ok"), any(CPFlowContext.class))).thenAnswer(invocation -> {
            ctxRef.set(invocation.getArgument(1, CPFlowContext.class));
            return liteflowResponse;
        });

        CPControllerDispatcherImpl dispatcher = new CPControllerDispatcherImpl(
                mapper,
                sessionCenterService,
                flowTxExecutor,
                mapper,
                applicationContext
        );

        TestSession session = new TestSession();
        session.setAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, 123L);
        session.setAttributeValue(CPConnectionAttributes.REMOTE_ADDRESS, "127.0.0.1:1");
        session.setAttributeValue(CPConnectionAttributes.REMOTE_IP, "127.0.0.1");
        session.setAttributeValue(CPConnectionAttributes.REMOTE_PORT, 1);

        CPResponse response = dispatcher.process("{\"id\":9,\"route\":\"/core/test/ok\",\"data\":{}}", session);
        assertEquals(200, response.getCode());
        assertEquals(9, response.getId());
        assertNotNull(response.getData());
        assertEquals("ok", response.getData().get("msg").asText());

        dispatcher.channelInactive(session);
        verify(sessionCenterService, times(1)).removeSession(123L, session);
    }

    @Test
    void process_invalidJson_shouldReturnError() {
        CPControllerDispatcherImpl dispatcher = newDispatcherWithBeans(Map.of("ok", new OkController()));
        CPResponse response = dispatcher.process("not-json", new TestSession());
        assertEquals(100, response.getCode());
    }

    @CPControllerTag(path = "/core/test/invalid-vo", voClazz = String.class, resultClazz = OkResult.class)
    public static class InvalidVoController {
    }

    @CPControllerTag(path = "/core/test/invalid-result", voClazz = OkVO.class, resultClazz = String.class)
    public static class InvalidResultController {
    }

    @Test
    void initControllerMaps_invalidTypes_shouldSkipRegistration() {
        CPControllerDispatcherImpl dispatcher = newDispatcherWithBeans(Map.of(
                "invalidVo", new InvalidVoController(),
                "invalidResult", new InvalidResultController()
        ));

        CPResponse resp1 = dispatcher.process("{\"id\":1,\"route\":\"/core/test/invalid-vo\",\"data\":{}}", new TestSession());
        assertEquals(404, resp1.getCode());

        CPResponse resp2 = dispatcher.process("{\"id\":2,\"route\":\"/core/test/invalid-result\",\"data\":{}}", new TestSession());
        assertEquals(404, resp2.getCode());
    }

    @Test
    void process_liteflowNotSuccess_shouldStillReturnResponse() {
        CPSessionCenterService sessionCenterService = mock(CPSessionCenterService.class);
        FlowTxExecutor flowTxExecutor = mock(FlowTxExecutor.class);
        ObjectMapper mapper = new ObjectMapper();
        ApplicationContext applicationContext = mock(ApplicationContext.class);

        when(applicationContext.getBeansWithAnnotation(CPControllerTag.class))
                .thenReturn(Map.of("ok", new OkController()));

        LiteflowResponse liteflowResponse = mock(LiteflowResponse.class);
        when(liteflowResponse.isSuccess()).thenReturn(false);
        when(liteflowResponse.getMessage()).thenReturn("failed");
        java.util.concurrent.atomic.AtomicReference<CPFlowContext> ctxRef = new java.util.concurrent.atomic.AtomicReference<>();
        when(liteflowResponse.getContextBean(eq(CPFlowContext.class))).thenAnswer(invocation -> ctxRef.get());
        when(flowTxExecutor.executeWithTx(eq("/core/test/ok"), any(CPFlowContext.class))).thenAnswer(invocation -> {
            ctxRef.set(invocation.getArgument(1, CPFlowContext.class));
            return liteflowResponse;
        });

        CPControllerDispatcherImpl dispatcher = new CPControllerDispatcherImpl(
                mapper,
                sessionCenterService,
                flowTxExecutor,
                mapper,
                applicationContext
        );

        CPResponse response = dispatcher.process("{\"id\":9,\"route\":\"/core/test/ok\",\"data\":{}}", new TestSession());
        assertEquals(200, response.getCode());
        assertEquals(9, response.getId());
        assertEquals("ok", response.getData().get("msg").asText());
    }

    @Test
    void process_resultClassMissing_shouldReturn404() throws Exception {
        CPSessionCenterService sessionCenterService = mock(CPSessionCenterService.class);
        FlowTxExecutor flowTxExecutor = mock(FlowTxExecutor.class);
        ObjectMapper mapper = new ObjectMapper();
        ApplicationContext applicationContext = mock(ApplicationContext.class);

        when(applicationContext.getBeansWithAnnotation(CPControllerTag.class))
                .thenReturn(Map.of("ok", new OkController()));

        LiteflowResponse liteflowResponse = mock(LiteflowResponse.class);
        when(liteflowResponse.isSuccess()).thenReturn(true);
        java.util.concurrent.atomic.AtomicReference<CPFlowContext> ctxRef = new java.util.concurrent.atomic.AtomicReference<>();
        when(liteflowResponse.getContextBean(eq(CPFlowContext.class))).thenAnswer(invocation -> ctxRef.get());
        when(flowTxExecutor.executeWithTx(eq("/core/test/ok"), any(CPFlowContext.class))).thenAnswer(invocation -> {
            ctxRef.set(invocation.getArgument(1, CPFlowContext.class));
            return liteflowResponse;
        });

        CPControllerDispatcherImpl dispatcher = new CPControllerDispatcherImpl(
                mapper,
                sessionCenterService,
                flowTxExecutor,
                mapper,
                applicationContext
        );

        Field f = CPControllerDispatcherImpl.class.getDeclaredField("controllerAndResultMap");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Class<?>> resultMap = (Map<String, Class<?>>) f.get(dispatcher);
        resultMap.remove("/core/test/ok");

        CPResponse response = dispatcher.process("{\"id\":9,\"route\":\"/core/test/ok\",\"data\":{}}", new TestSession());
        assertEquals(404, response.getCode());
    }

    @Test
    void channelInactive_noUid_shouldNotRemoveSession() {
        CPSessionCenterService sessionCenterService = mock(CPSessionCenterService.class);
        FlowTxExecutor flowTxExecutor = mock(FlowTxExecutor.class);
        ObjectMapper mapper = new ObjectMapper();
        ApplicationContext applicationContext = mock(ApplicationContext.class);

        when(applicationContext.getBeansWithAnnotation(CPControllerTag.class))
                .thenReturn(Map.of("ok", new OkController()));

        LiteflowResponse liteflowResponse = mock(LiteflowResponse.class);
        when(liteflowResponse.isSuccess()).thenReturn(true);
        java.util.concurrent.atomic.AtomicReference<CPFlowContext> ctxRef = new java.util.concurrent.atomic.AtomicReference<>();
        when(liteflowResponse.getContextBean(eq(CPFlowContext.class))).thenAnswer(invocation -> ctxRef.get());
        when(flowTxExecutor.executeWithTx(eq("/core/test/ok"), any(CPFlowContext.class))).thenAnswer(invocation -> {
            ctxRef.set(invocation.getArgument(1, CPFlowContext.class));
            return liteflowResponse;
        });

        CPControllerDispatcherImpl dispatcher = new CPControllerDispatcherImpl(
                mapper,
                sessionCenterService,
                flowTxExecutor,
                mapper,
                applicationContext
        );

        TestSession session = new TestSession();
        dispatcher.channelInactive(session);
        verify(sessionCenterService, never()).removeSession(anyLong(), any());
    }

    @Test
    void process_noRemoteAttributes_shouldPassNullConnectionInfo() {
        CPSessionCenterService sessionCenterService = mock(CPSessionCenterService.class);
        FlowTxExecutor flowTxExecutor = mock(FlowTxExecutor.class);
        ObjectMapper mapper = new ObjectMapper();
        ApplicationContext applicationContext = mock(ApplicationContext.class);

        when(applicationContext.getBeansWithAnnotation(CPControllerTag.class))
                .thenReturn(Map.of("ok", new OkController()));

        LiteflowResponse liteflowResponse = mock(LiteflowResponse.class);
        when(liteflowResponse.isSuccess()).thenReturn(true);
        java.util.concurrent.atomic.AtomicReference<CPFlowContext> ctxRef = new java.util.concurrent.atomic.AtomicReference<>();
        when(liteflowResponse.getContextBean(eq(CPFlowContext.class))).thenAnswer(invocation -> ctxRef.get());
        when(flowTxExecutor.executeWithTx(eq("/core/test/ok"), any(CPFlowContext.class))).thenAnswer(invocation -> {
            CPFlowContext ctx = invocation.getArgument(1, CPFlowContext.class);
            ctxRef.set(ctx);
            return liteflowResponse;
        });

        CPControllerDispatcherImpl dispatcher = new CPControllerDispatcherImpl(
                mapper,
                sessionCenterService,
                flowTxExecutor,
                mapper,
                applicationContext
        );

        TestSession session = new TestSession();
        session.setAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, 1L);

        CPResponse response = dispatcher.process("{\"id\":1,\"route\":\"/core/test/ok\",\"data\":{}}", session);
        assertEquals(200, response.getCode());
        assertNotNull(ctxRef.get());
        assertNull(ctxRef.get().getConnectionInfo());
    }

    private static CPControllerDispatcherImpl newDispatcherWithBeans(Map<String, Object> beans) {
        CPSessionCenterService sessionCenterService = mock(CPSessionCenterService.class);
        FlowTxExecutor flowTxExecutor = mock(FlowTxExecutor.class);
        ObjectMapper mapper = new ObjectMapper();
        ApplicationContext applicationContext = mock(ApplicationContext.class);

        when(applicationContext.getBeansWithAnnotation(CPControllerTag.class)).thenReturn(beans);

        LiteflowResponse liteflowResponse = mock(LiteflowResponse.class);
        when(liteflowResponse.isSuccess()).thenReturn(true);
        java.util.concurrent.atomic.AtomicReference<CPFlowContext> ctxRef = new java.util.concurrent.atomic.AtomicReference<>();
        when(liteflowResponse.getContextBean(eq(CPFlowContext.class))).thenAnswer(invocation -> ctxRef.get());
        when(flowTxExecutor.executeWithTx(anyString(), any(CPFlowContext.class))).thenAnswer(invocation -> {
            ctxRef.set(invocation.getArgument(1, CPFlowContext.class));
            return liteflowResponse;
        });

        return new CPControllerDispatcherImpl(
                mapper,
                sessionCenterService,
                flowTxExecutor,
                mapper,
                applicationContext
        );
    }
}
