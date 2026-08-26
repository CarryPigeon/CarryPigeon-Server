package team.carrypigeon.backend.chat.domain.features.channel.controller.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.HandlerInterceptor;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ChannelLifecycleApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.CreateChannelCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.DeleteChannelCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.UpdateChannelProfileCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelResult;
import team.carrypigeon.backend.chat.domain.shared.controller.advice.GlobalExceptionHandler;
import team.carrypigeon.backend.chat.domain.shared.controller.support.RequestAuthenticationContext;
import team.carrypigeon.backend.chat.domain.shared.domain.auth.AuthenticatedAccount;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ChannelLifecycleController 协议测试。
 * 职责：验证频道创建、删除和资料更新的 HTTP 命令映射。
 * 边界：领域生命周期操作由替身提供，不验证业务规则。
 */
@Tag("contract")
class ChannelLifecycleControllerTests {

    private ChannelLifecycleApi channelLifecycleDomainApi;
    private RequestAuthenticationContext authRequestContext;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        channelLifecycleDomainApi = mock(ChannelLifecycleApi.class);
        authRequestContext = new RequestAuthenticationContext();
        mockMvc = authenticatedMockMvc();
    }

    /**
     * 验证创建频道返回 201 并完整映射当前账户与请求体。
     */
    @Test
    @DisplayName("create channel returns 201")
    void createChannel_returns201() throws Exception {
        when(channelLifecycleDomainApi.createChannel(any())).thenReturn(channel());

        mockMvc.perform(post("/api/channels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"project-alpha","brief":"讨论区","avatar":"avatars/ch/9.png"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cid").value("9"))
                .andExpect(jsonPath("$.brief").value("讨论区"));
        ArgumentCaptor<CreateChannelCommand> captor = ArgumentCaptor.forClass(CreateChannelCommand.class);
        verify(channelLifecycleDomainApi).createChannel(captor.capture());
        assertEquals(1001L, captor.getValue().accountId());
        assertEquals("project-alpha", captor.getValue().name());
        assertEquals("讨论区", captor.getValue().brief());
        assertEquals("avatars/ch/9.png", captor.getValue().avatar());
    }

    /**
     * 验证删除频道返回 204 并映射操作者与频道 ID。
     */
    @Test
    @DisplayName("delete channel returns 204")
    void deleteChannel_returns204() throws Exception {
        doNothing().when(channelLifecycleDomainApi).deleteChannel(any());

        mockMvc.perform(delete("/api/channels/9"))
                .andExpect(status().isNoContent());
        ArgumentCaptor<DeleteChannelCommand> captor = ArgumentCaptor.forClass(DeleteChannelCommand.class);
        verify(channelLifecycleDomainApi).deleteChannel(captor.capture());
        assertEquals(1001L, captor.getValue().operatorAccountId());
        assertEquals(9L, captor.getValue().channelId());
    }

    /**
     * 验证更新频道资料返回 204 并映射路径与请求体字段。
     */
    @Test
    @DisplayName("patch channel profile returns 204")
    void patchChannelProfile_returns204() throws Exception {
        when(channelLifecycleDomainApi.updateChannelProfile(any())).thenReturn(channel());

        mockMvc.perform(patch("/api/channels/9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"project-alpha","brief":"new brief"}
                                """))
                .andExpect(status().isNoContent());
        ArgumentCaptor<UpdateChannelProfileCommand> captor =
                ArgumentCaptor.forClass(UpdateChannelProfileCommand.class);
        verify(channelLifecycleDomainApi).updateChannelProfile(captor.capture());
        assertEquals(1001L, captor.getValue().operatorAccountId());
        assertEquals(9L, captor.getValue().channelId());
        assertEquals("project-alpha", captor.getValue().name());
        assertEquals("new brief", captor.getValue().brief());
    }

    private ChannelResult channel() {
        return new ChannelResult(
                9L, 9L, "project-alpha", "讨论区", "avatars/ch/9.png", "1001", "private", false,
                Instant.parse("2026-04-24T12:00:00Z"), Instant.parse("2026-04-24T12:00:00Z")
        );
    }

    private MockMvc authenticatedMockMvc() {
        return MockMvcBuilders.standaloneSetup(
                        new ChannelLifecycleController(channelLifecycleDomainApi, authRequestContext)
                )
                .addInterceptors(new BindPrincipalInterceptor(authRequestContext))
                .setMessageConverters(snakeCaseConverter())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private MappingJackson2HttpMessageConverter snakeCaseConverter() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        return new MappingJackson2HttpMessageConverter(objectMapper);
    }

    /** 测试用认证主体绑定拦截器。 */
    private static class BindPrincipalInterceptor implements HandlerInterceptor {
        private final RequestAuthenticationContext authRequestContext;

        private BindPrincipalInterceptor(RequestAuthenticationContext authRequestContext) {
            this.authRequestContext = authRequestContext;
        }

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            authRequestContext.bind(request, new AuthenticatedAccount(1001L, "carry-user"));
            return true;
        }
    }
}
