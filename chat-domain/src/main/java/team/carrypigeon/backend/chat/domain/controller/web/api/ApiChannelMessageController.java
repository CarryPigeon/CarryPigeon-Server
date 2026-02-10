package team.carrypigeon.backend.chat.domain.controller.web.api;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;
import team.carrypigeon.backend.chat.domain.controller.web.api.auth.ApiAuth;
import team.carrypigeon.backend.chat.domain.controller.web.api.dto.MessageCreateRequest;
import team.carrypigeon.backend.chat.domain.controller.web.api.dto.MessageDeleteRequest;
import team.carrypigeon.backend.chat.domain.controller.web.api.dto.MessageListRequest;
import team.carrypigeon.backend.chat.domain.controller.web.api.dto.ReadStateUpdateRequest;
import team.carrypigeon.backend.chat.domain.controller.web.api.flow.ApiFlowRunner;

/**
 * 频道消息 API 控制器。
 * <p>
 * 提供频道列表、消息列表、消息创建、消息删除、已读状态更新与未读统计路由。
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

    /**
     * 构造频道消息控制器。
     *
     * @param flowRunner API 责任链执行器。
     */
    public ApiChannelMessageController(ApiFlowRunner flowRunner) {
        this.flowRunner = flowRunner;
    }

    /**
     * 查询当前用户可访问的频道列表。
     *
     * @param request HTTP 请求对象。
     * @return 标准频道列表响应。
     */
    @GetMapping("/api/channels")
    public Object channels(HttpServletRequest request) {
        CPFlowContext ctx = new CPFlowContext();
        ctx.set(CPFlowKeys.AUTH_UID, ApiAuth.requireUid(request));
        flowRunner.executeOrThrow(CHAIN_CHANNELS_LIST, ctx);
        return ctx.get(CPFlowKeys.RESPONSE);
    }

    /**
     * 分页查询频道消息列表。
     *
     * @param cid 频道 ID。
     * @param cursor 游标。
     * @param limit 每页数量。
     * @param request HTTP 请求对象。
     * @return 标准消息列表响应。
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
     * 在指定频道创建消息。
     *
     * @param cid 频道 ID。
     * @param body 创建消息请求体。
     * @param idempotencyKey 幂等键。
     * @param request HTTP 请求对象。
     * @return HTTP 201 与标准消息创建响应。
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
     * 删除指定消息。
     *
     * @param mid 消息 ID。
     * @param request HTTP 请求对象。
     * @return HTTP 204 无内容响应。
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
     * 更新频道已读游标。
     *
     * @param cid 频道 ID。
     * @param body 已读状态请求体。
     * @param request HTTP 请求对象。
     * @return 标准已读状态响应。
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
     * 查询当前用户可见频道的未读统计。
     *
     * @param request HTTP 请求对象。
     * @return 标准未读统计响应。
     */
    @GetMapping("/api/unreads")
    public Object unreads(HttpServletRequest request) {
        CPFlowContext ctx = new CPFlowContext();
        ctx.set(CPFlowKeys.AUTH_UID, ApiAuth.requireUid(request));
        flowRunner.executeOrThrow(CHAIN_UNREADS_LIST, ctx);
        return ctx.get(CPFlowKeys.RESPONSE);
    }

    /**
     * 创建消息请求体。
     *
     * @param domain 消息领域标识。
     * @param domainVersion 消息领域版本。
     * @param replyToMid 回复目标消息 ID。
     * @param data 消息载荷。
     */
    public record CreateMessageBody(@NotBlank String domain,
                                    String domainVersion,
                                    String replyToMid,
                                    @NotNull JsonNode data) {
    }

    /**
     * 已读状态更新请求体。
     *
     * @param lastReadMid 最后一条已读消息 ID。
     * @param lastReadTime 最后已读时间戳。
     */
    public record ReadStateBody(@NotBlank String lastReadMid, Long lastReadTime) {
    }
}
