package team.carrypigeon.backend.chat.domain.features.message.controller.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.HandlerInterceptor;
import team.carrypigeon.backend.chat.domain.features.auth.controller.support.AuthRequestContext;
import team.carrypigeon.backend.chat.domain.features.auth.controller.support.AuthenticatedPrincipal;
import team.carrypigeon.backend.chat.domain.features.message.application.dto.MentionResult;
import team.carrypigeon.backend.chat.domain.features.message.application.query.ListMentionsQuery;
import team.carrypigeon.backend.chat.domain.features.message.application.service.MentionApplicationService;
import team.carrypigeon.backend.chat.domain.shared.controller.OpaqueCursorCodec;
import team.carrypigeon.backend.chat.domain.shared.controller.advice.GlobalExceptionHandler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@Tag("contract")
class MentionControllerTests {

    private static final String MENTION_CURSOR_SCOPE = "mentions";

    private MentionApplicationService mentionApplicationService;
    private AuthRequestContext authRequestContext;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mentionApplicationService = mock(MentionApplicationService.class);
        authRequestContext = new AuthRequestContext();
        mockMvc = authenticatedMockMvc();
    }

    @Test
    @DisplayName("list mentions returns paged payload")
    void listMentions_returnsPagedPayload() throws Exception {
        when(mentionApplicationService.listMentions(any())).thenReturn(List.of(
                mention(13L, false),
                mention(12L, true),
                mention(11L, false)
        ));

        mockMvc.perform(get("/api/mentions").param("limit", "2").param("unread_only", "true").param("cid", "9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].mention_id").value("13"))
                .andExpect(jsonPath("$.items[0].cid").value("9"))
                .andExpect(jsonPath("$.items[0].target.type").value("user"))
                .andExpect(jsonPath("$.items[1].mention_id").value("12"))
                .andExpect(jsonPath("$.next_cursor").value(OpaqueCursorCodec.encode(MENTION_CURSOR_SCOPE, 12L)))
                .andExpect(jsonPath("$.has_more").value(true));

        ArgumentCaptor<ListMentionsQuery> captor = ArgumentCaptor.forClass(ListMentionsQuery.class);
        verify(mentionApplicationService).listMentions(captor.capture());
        assertEquals(1001L, captor.getValue().accountId());
        assertNull(captor.getValue().cursorMentionId());
        assertEquals(2, captor.getValue().limit());
        assertEquals(true, captor.getValue().unreadOnly());
        assertEquals(9L, captor.getValue().channelId());
    }

    @Test
    @DisplayName("list mentions invalid limit returns bad request")
    void listMentions_invalidLimit_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/mentions").param("limit", "0"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.reason").value("validation_failed"));
    }

    @Test
    @DisplayName("list mentions limit too large returns bad request")
    void listMentions_limitTooLarge_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/mentions").param("limit", "51"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.reason").value("validation_failed"))
                .andExpect(jsonPath("$.error.message").value("limit must be between 1 and 50"));
    }

    @Test
    @DisplayName("list mentions invalid cursor returns cursor invalid")
    void listMentions_invalidCursor_returnsCursorInvalid() throws Exception {
        mockMvc.perform(get("/api/mentions").param("cursor", "abc"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.reason").value("cursor_invalid"));
    }

    @Test
    @DisplayName("list mentions invalid cid returns validation failed")
    void listMentions_invalidCid_returnsValidationFailed() throws Exception {
        mockMvc.perform(get("/api/mentions").param("cid", "abc"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.reason").value("validation_failed"))
                .andExpect(jsonPath("$.error.message").value("cid must be decimal snowflake string"));
    }

    @Test
    @DisplayName("list mentions forwards trimmed cursor and cid")
    void listMentions_forwardsTrimmedCursorAndCid() throws Exception {
        when(mentionApplicationService.listMentions(any())).thenReturn(List.of(mention(13L, false)));

        mockMvc.perform(get("/api/mentions")
                        .param("cursor", OpaqueCursorCodec.encode(MENTION_CURSOR_SCOPE, 88L))
                        .param("cid", " 9 ")
                        .param("unread_only", "false"))
                .andExpect(status().isOk());

        ArgumentCaptor<ListMentionsQuery> captor = ArgumentCaptor.forClass(ListMentionsQuery.class);
        verify(mentionApplicationService).listMentions(captor.capture());
        assertEquals(88L, captor.getValue().cursorMentionId());
        assertEquals(9L, captor.getValue().channelId());
        assertEquals(false, captor.getValue().unreadOnly());
        assertEquals(20, captor.getValue().limit());
    }

    @Test
    @DisplayName("mark mention read returns no content")
    void markMentionRead_returnsNoContent() throws Exception {
        doNothing().when(mentionApplicationService).markMentionRead(1001L, 11L);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/mentions/11/read"))
                .andExpect(status().isNoContent());

        verify(mentionApplicationService).markMentionRead(1001L, 11L);
    }

    @Test
    @DisplayName("mark mentions read returns no content")
    void markMentionsRead_returnsNoContent() throws Exception {
        doNothing().when(mentionApplicationService).markMentionsRead(1001L, 88L, 9L);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/mentions/read_state")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"before_mention_id\":\"88\"," +
                                "\"cid\":\"9\"" +
                                "}"))
                .andExpect(status().isNoContent());

        verify(mentionApplicationService).markMentionsRead(1001L, 88L, 9L);
    }

    @Test
    @DisplayName("mark mentions read empty body returns no content")
    void markMentionsRead_emptyBody_returnsNoContent() throws Exception {
        doNothing().when(mentionApplicationService).markMentionsRead(1001L, null, null);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/mentions/read_state"))
                .andExpect(status().isNoContent());

        verify(mentionApplicationService).markMentionsRead(1001L, null, null);
    }

    private MockMvc authenticatedMockMvc() {
        return MockMvcBuilders.standaloneSetup(new MentionController(mentionApplicationService, authRequestContext))
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

    private MentionResult mention(long mentionId, boolean read) {
        return new MentionResult(mentionId, 9L, 5001L + mentionId, 1002L, "user", 1001L, Instant.parse("2026-04-24T12:00:00Z"), read);
    }

    private static class BindPrincipalInterceptor implements HandlerInterceptor {
        private final AuthRequestContext authRequestContext;

        private BindPrincipalInterceptor(AuthRequestContext authRequestContext) {
            this.authRequestContext = authRequestContext;
        }

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            authRequestContext.bind(request, new AuthenticatedPrincipal(1001L, "carry-user"));
            return true;
        }
    }
}
