package team.carrypigeon.backend.chat.domain.controller.web.api;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.chat.domain.controller.web.api.auth.ApiAuth;
import team.carrypigeon.backend.chat.domain.controller.web.api.dto.MessageCreateRequest;
import team.carrypigeon.backend.chat.domain.controller.web.api.dto.MessageDeleteRequest;
import team.carrypigeon.backend.chat.domain.controller.web.api.dto.MessageListRequest;
import team.carrypigeon.backend.chat.domain.controller.web.api.dto.ReadStateUpdateRequest;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;
import team.carrypigeon.backend.chat.domain.controller.web.api.flow.ApiFlowRunner;

/**
 * Channel and message resources under {@code /api}.
 * <p>
 * All routes in this controller require {@code Authorization: Bearer <access_token>}.
 * Business logic is implemented in LiteFlow chains defined in {@code application-starter/src/main/resources/config/api.xml}.
 * <p>
 * Controller responsibilities:
 * <ul>
 *   <li>Extract path/query parameters and JSON body;</li>
 *   <li>Build a request DTO and store it under {@link ApiFlowKeys#REQUEST};</li>
 *   <li>Execute chain via {@link ApiFlowRunner};</li>
 *   <li>Return object from {@link ApiFlowKeys#RESPONSE} (or set HTTP status in controller when needed).</li>
 * </ul>
 */
@RestController
public class ApiChannelMessageController {

    private static final String CHAIN_CHANNELS_LIST = "api_channels_list";
    private static final String CHAIN_MESSAGES_LIST = "api_messages_list";
    private static final String CHAIN_MESSAGES_CREATE = "api_messages_create";
    private static final String CHAIN_MESSAGES_DELETE = "api_messages_delete";
    private static final String CHAIN_READ_STATE_UPDATE = "api_read_state_update";
    private static final String CHAIN_UNREADS_LIST = "api_unreads_list";

    private final ApiFlowRunner flowRunner;

    public ApiChannelMessageController(ApiFlowRunner flowRunner) {
        this.flowRunner = flowRunner;
    }

    /**
     * List channels current user can access.
     * <p>
     * Route: {@code GET /api/channels}
     * <p>
     * Chain: {@code api_channels_list}
     */
    @GetMapping("/api/channels")
    public Object channels(HttpServletRequest request) {
        CPFlowContext ctx = new CPFlowContext();
        ctx.set(CPFlowKeys.AUTH_UID, ApiAuth.requireUid(request));
        flowRunner.executeOrThrow(CHAIN_CHANNELS_LIST, ctx);
        return ctx.get(CPFlowKeys.RESPONSE);
    }

    /**
     * List messages in a channel with cursor pagination.
     * <p>
     * Route: {@code GET /api/channels/{cid}/messages}
     * <p>
     * Chain: {@code api_messages_list}
     */
    @GetMapping("/api/channels/{cid}/messages")
    public Object messages(@PathVariable("cid") String cid,
                           @RequestParam(value = "cursor", required = false) String cursor,
                           @RequestParam(value = "limit", required = false) Integer limit,
                           HttpServletRequest request) {
        CPFlowContext ctx = new CPFlowContext();
        ctx.set(CPFlowKeys.REQUEST, new MessageListRequest(cid, cursor, limit));
        ctx.set(CPFlowKeys.AUTH_UID, ApiAuth.requireUid(request));
        flowRunner.executeOrThrow(CHAIN_MESSAGES_LIST, ctx);
        return ctx.get(CPFlowKeys.RESPONSE);
    }

    /**
     * Create a message in a channel.
     * <p>
     * Route: {@code POST /api/channels/{cid}/messages}
     * <p>
     * Chain: {@code api_messages_create}
     * <p>
     * HTTP status: 201 on success.
     */
    @PostMapping("/api/channels/{cid}/messages")
    public ResponseEntity<Object> create(@PathVariable("cid") String cid,
                                         @Valid @RequestBody CreateMessageBody body,
                                         @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                         HttpServletRequest request) {
        CPFlowContext ctx = new CPFlowContext();
        ctx.set(CPFlowKeys.REQUEST, new MessageCreateRequest(cid, body.domain(), body.domainVersion(), body.replyToMid(), body.data(), idempotencyKey));
        ctx.set(CPFlowKeys.AUTH_UID, ApiAuth.requireUid(request));
        flowRunner.executeOrThrow(CHAIN_MESSAGES_CREATE, ctx);
        return ResponseEntity.status(201).body(ctx.get(CPFlowKeys.RESPONSE));
    }

    /**
     * Delete a message.
     * <p>
     * Route: {@code DELETE /api/messages/{mid}}
     * <p>
     * Chain: {@code api_messages_delete}
     */
    @DeleteMapping("/api/messages/{mid}")
    public ResponseEntity<Void> delete(@PathVariable("mid") String mid, HttpServletRequest request) {
        CPFlowContext ctx = new CPFlowContext();
        ctx.set(CPFlowKeys.REQUEST, new MessageDeleteRequest(mid));
        ctx.set(CPFlowKeys.AUTH_UID, ApiAuth.requireUid(request));
        flowRunner.executeOrThrow(CHAIN_MESSAGES_DELETE, ctx);
        return ResponseEntity.noContent().build();
    }

    /**
     * Update read state for a channel.
     * <p>
     * Route: {@code PUT /api/channels/{cid}/read_state}
     * <p>
     * Chain: {@code api_read_state_update}
     */
    @PutMapping("/api/channels/{cid}/read_state")
    public Object updateReadState(@PathVariable("cid") String cid,
                                  @Valid @RequestBody ReadStateBody body,
                                  HttpServletRequest request) {
        CPFlowContext ctx = new CPFlowContext();
        ctx.set(CPFlowKeys.REQUEST, new ReadStateUpdateRequest(cid, body.lastReadMid(), body.lastReadTime()));
        ctx.set(CPFlowKeys.AUTH_UID, ApiAuth.requireUid(request));
        flowRunner.executeOrThrow(CHAIN_READ_STATE_UPDATE, ctx);
        return ctx.get(CPFlowKeys.RESPONSE);
    }

    /**
     * List unread counts for each channel visible to the current user.
     * <p>
     * Route: {@code GET /api/unreads}
     * <p>
     * Chain: {@code api_unreads_list}
     */
    @GetMapping("/api/unreads")
    public Object unreads(HttpServletRequest request) {
        CPFlowContext ctx = new CPFlowContext();
        ctx.set(CPFlowKeys.AUTH_UID, ApiAuth.requireUid(request));
        flowRunner.executeOrThrow(CHAIN_UNREADS_LIST, ctx);
        return ctx.get(CPFlowKeys.RESPONSE);
    }

    /**
     * JSON body for {@code POST /api/channels/{cid}/messages}.
     */
    public record CreateMessageBody(@NotBlank String domain,
                                    String domainVersion,
                                    String replyToMid,
                                    @NotNull JsonNode data) {
    }

    /**
     * JSON body for {@code PUT /api/channels/{cid}/read_state}.
     */
    public record ReadStateBody(@NotBlank String lastReadMid, Long lastReadTime) {
    }
}
