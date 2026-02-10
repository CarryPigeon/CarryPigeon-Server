package team.carrypigeon.backend.chat.domain.controller.web.api.ws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemReason;
import team.carrypigeon.backend.api.starter.server.ServerInfoConfig;
import team.carrypigeon.backend.chat.domain.controller.web.api.auth.AccessTokenService;
import team.carrypigeon.backend.chat.domain.controller.web.api.config.CpApiProperties;
import team.carrypigeon.backend.chat.domain.service.ws.ApiWsEventPublisher;
import team.carrypigeon.backend.chat.domain.service.ws.ApiWsEventStore;
import team.carrypigeon.backend.chat.domain.service.ws.ApiWsSessionAttributes;
import team.carrypigeon.backend.chat.domain.service.ws.ApiWsSessionRegistry;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * `/api/ws` WebSocket 协议处理器。
 * <p>
 * 该类实现消息级状态机，负责 `ping`、`auth`、`reauth`、`subscribe` 指令分发，
 * 并在鉴权成功后与会话注册表、事件存储、事件发布器协作完成在线推送与断线回放。
 */
@Slf4j
@Component
public class ApiWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final CpApiProperties properties;
    private final AccessTokenService accessTokenService;
    private final ServerInfoConfig serverInfoConfig;
    private final ApiWsSessionRegistry sessionRegistry;
    private final ApiWsEventStore eventStore;
    private final ApiWsEventPublisher eventPublisher;

    /**
     * 构造 WebSocket 协议处理器。
     *
     * @param objectMapper JSON 序列化与反序列化组件。
     * @param properties API 配置，用于读取回放阈值等参数。
     * @param accessTokenService Access Token 校验服务。
     * @param serverInfoConfig 当前服务节点信息。
     * @param sessionRegistry WebSocket 会话注册表。
     * @param eventStore WebSocket 事件存储，用于断线回放。
     * @param eventPublisher 事件封包发布器。
     */
    public ApiWebSocketHandler(ObjectMapper objectMapper,
                               CpApiProperties properties,
                               AccessTokenService accessTokenService,
                               ServerInfoConfig serverInfoConfig,
                               ApiWsSessionRegistry sessionRegistry,
                               ApiWsEventStore eventStore,
                               ApiWsEventPublisher eventPublisher) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.accessTokenService = accessTokenService;
        this.serverInfoConfig = serverInfoConfig;
        this.sessionRegistry = sessionRegistry;
        this.eventStore = eventStore;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 处理连接建立事件，仅记录连接元信息。
     *
     * @param session 已建立的 WebSocket 会话。
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("WS 连接建立：sessionId={}, ip={}", session.getId(), remoteIp(session));
    }

    /**
     * 处理连接关闭事件，执行会话反注册。
     *
     * @param session 已关闭的 WebSocket 会话。
     * @param status 关闭状态码与原因。
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessionRegistry.unregister(session);
        log.info("WS 连接关闭：sessionId={}, status={}", session.getId(), status);
    }

    /**
     * 解析入站消息并按指令类型分发处理。
     *
     * @param session 当前 WebSocket 会话。
     * @param message 客户端发送的文本消息。
     * @throws Exception 当下游处理或发送消息失败时抛出。
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode root;
        try {
            root = objectMapper.readTree(message.getPayload());
        } catch (JsonProcessingException e) {
            log.debug("WS 收到非法 JSON：sessionId={}", session.getId());
            return;
        }
        if (root == null || !root.isObject()) {
            return;
        }

        String type = text(root.get("type"));
        String id = text(root.get("id"));
        JsonNode data = root.get("data");
        if (type == null || type.isBlank()) {
            return;
        }

        switch (type) {
            case "ping" -> send(session, objectMapper.createObjectNode().put("type", "pong"));
            case "auth" -> handleAuth(session, id, data);
            case "reauth" -> handleReauth(session, id, data);
            case "subscribe" -> handleSubscribe(session, id, data);
            default -> {
            }
        }
    }

    /**
     * 处理 `auth` 指令，完成会话鉴权绑定并按需执行断线回放。
     *
     * @param session 当前 WebSocket 会话。
     * @param id 客户端消息标识。
     * @param data 指令参数体。
     * @throws Exception 当回包或回放发送失败时抛出。
     */
    private void handleAuth(WebSocketSession session, String id, JsonNode data) throws Exception {
        if (data == null || !data.isObject()) {
            sendErr(session, "auth", id, CPProblemReason.VALIDATION_FAILED, "validation failed", null);
            return;
        }
        Integer apiVersion = data.has("api_version") && data.get("api_version").canConvertToInt()
                ? data.get("api_version").asInt()
                : null;
        if (apiVersion != null && apiVersion != 1) {
            sendErr(session, "auth", id, CPProblemReason.API_VERSION_UNSUPPORTED, "api version unsupported", null);
            return;
        }

        String accessToken = text(data.get("access_token"));
        AccessTokenService.TokenInfo info = accessTokenService.verifyInfo(accessToken);
        if (info == null) {
            sendErr(session, "auth", id, CPProblemReason.UNAUTHORIZED, "missing or invalid access token", null);
            return;
        }

        long uid = info.uid();
        session.getAttributes().put(ApiWsSessionAttributes.UID, uid);
        sessionRegistry.register(uid, session);

        ObjectNode okData = objectMapper.createObjectNode();
        okData.put("uid", Long.toString(uid));
        okData.put("expires_at", info.expiresAt());
        okData.put("server_id", serverInfoConfig.getServerId());
        sendOk(session, "auth", id, okData);

        JsonNode resume = data.get("resume");
        String lastEventId = resume != null && resume.isObject() ? text(resume.get("last_event_id")) : null;
        if (lastEventId != null && !lastEventId.isBlank()) {
            resumeOrFail(session, lastEventId);
        }
    }

    /**
     * 处理 `reauth` 指令，刷新会话令牌并在用户切换时重建绑定关系。
     *
     * @param session 当前 WebSocket 会话。
     * @param id 客户端消息标识。
     * @param data 指令参数体。
     * @throws Exception 当回包失败时抛出。
     */
    private void handleReauth(WebSocketSession session, String id, JsonNode data) throws Exception {
        if (data == null || !data.isObject()) {
            sendErr(session, "reauth", id, CPProblemReason.VALIDATION_FAILED, "validation failed", null);
            return;
        }
        String accessToken = text(data.get("access_token"));
        AccessTokenService.TokenInfo info = accessTokenService.verifyInfo(accessToken);
        if (info == null) {
            sendErr(session, "reauth", id, CPProblemReason.UNAUTHORIZED, "missing or invalid access token", null);
            return;
        }
        long uid = info.uid();

        Long oldUid = uidOf(session);
        if (oldUid == null || oldUid != uid) {
            sessionRegistry.unregister(session);
            session.getAttributes().put(ApiWsSessionAttributes.UID, uid);
            sessionRegistry.register(uid, session);
        }

        ObjectNode okData = objectMapper.createObjectNode();
        okData.put("uid", Long.toString(uid));
        okData.put("expires_at", info.expiresAt());
        okData.put("server_id", serverInfoConfig.getServerId());
        sendOk(session, "reauth", id, okData);
    }

    /**
     * 处理 `subscribe` 指令，设置或清理频道级订阅过滤。
     *
     * @param session 当前 WebSocket 会话。
     * @param id 客户端消息标识。
     * @param data 指令参数体。
     * @throws Exception 当回包失败时抛出。
     */
    private void handleSubscribe(WebSocketSession session, String id, JsonNode data) throws Exception {
        if (uidOf(session) == null) {
            sendErr(session, "subscribe", id, CPProblemReason.UNAUTHORIZED, "unauthorized", null);
            return;
        }
        Set<Long> cids = Set.of();
        if (data != null && data.isObject() && data.has("cids") && data.get("cids").isArray()) {
            cids = streamTextArray(data.get("cids")).stream()
                    .map(this::parseLongOrNull)
                    .filter(v -> v != null && v > 0)
                    .collect(Collectors.toSet());
        }
        if (cids.isEmpty()) {
            session.getAttributes().remove(ApiWsSessionAttributes.SUB_CIDS);
        } else {
            session.getAttributes().put(ApiWsSessionAttributes.SUB_CIDS, cids);
        }
        ObjectNode okData = objectMapper.createObjectNode();
        okData.set("cids", objectMapper.valueToTree(cids.stream().map(Object::toString).toList()));
        sendOk(session, "subscribe", id, okData);
    }

    /**
     * 尝试回放 `last_event_id` 之后的事件；回放失败时发送 `resume.failed`。
     *
     * @param session 当前 WebSocket 会话。
     * @param lastEventId 客户端已接收的最后事件 ID。
     * @throws Exception 当消息发送失败时抛出。
     */
    private void resumeOrFail(WebSocketSession session, String lastEventId) throws Exception {
        ApiWsEventStore.ResumeResult r = eventStore.resumeAfter(lastEventId, properties.getApi().getWs().getResumeMaxEvents());
        if (r.failedReason() != null) {
            ObjectNode data = objectMapper.createObjectNode();
            data.put("reason", r.failedReason());
            send(session, objectMapper.createObjectNode().put("type", "resume.failed").set("data", data));
            return;
        }
        for (ApiWsEventStore.StoredEvent e : r.events()) {
            send(session, eventPublisher.toEventEnvelope(e));
        }
    }

    /**
     * 从会话属性中读取当前绑定用户 ID。
     *
     * @param session WebSocket 会话。
     * @return 用户 ID；不存在或类型不匹配时返回 {@code null}。
     */
    private Long uidOf(WebSocketSession session) {
        Object v = session.getAttributes().get(ApiWsSessionAttributes.UID);
        if (v instanceof Long l) {
            return l;
        }
        if (v instanceof Integer i) {
            return i.longValue();
        }
        return null;
    }

    /**
     * 将字符串解析为长整型。
     *
     * @param s 待解析字符串。
     * @return 解析后的数值；为空或格式非法时返回 {@code null}。
     */
    private Long parseLongOrNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(s);
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * 将 JSON 数组元素提取为字符串列表。
     *
     * @param arr JSON 数组节点。
     * @return 字符串列表，忽略无法转换为文本的空值元素。
     */
    private List<String> streamTextArray(JsonNode arr) {
        java.util.ArrayList<String> out = new java.util.ArrayList<>();
        for (JsonNode n : arr) {
            String v = text(n);
            if (v != null) {
                out.add(v);
            }
        }
        return out;
    }

    /**
     * 发送命令成功响应 `{cmd}.ok`。
     *
     * @param session 目标 WebSocket 会话。
     * @param cmd 指令名称。
     * @param id 客户端消息标识。
     * @param data 成功响应数据。
     * @throws Exception 当消息发送失败时抛出。
     */
    private void sendOk(WebSocketSession session, String cmd, String id, JsonNode data) throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("type", cmd + ".ok");
        if (id != null) {
            root.put("id", id);
        }
        if (data != null) {
            root.set("data", data);
        } else {
            root.set("data", objectMapper.createObjectNode());
        }
        send(session, root);
    }

    /**
     * 基于标准错误枚举发送命令失败响应。
     *
     * @param session 目标 WebSocket 会话。
     * @param cmd 指令名称。
     * @param id 客户端消息标识。
     * @param reason 标准错误原因枚举。
     * @param message 错误描述。
     * @param details 扩展错误详情。
     * @throws Exception 当消息发送失败时抛出。
     */
    private void sendErr(WebSocketSession session,
                         String cmd,
                         String id,
                         CPProblemReason reason,
                         String message,
                         Map<String, Object> details) throws Exception {
        CPProblemReason safeReason = reason != null ? reason : CPProblemReason.INTERNAL_ERROR;
        sendErr(session, cmd, id, safeReason.code(), message, details);
    }

    /**
     * 发送命令失败响应 `{cmd}.err`。
     *
     * @param session 目标 WebSocket 会话。
     * @param cmd 指令名称。
     * @param id 客户端消息标识。
     * @param reason 错误代码。
     * @param message 错误描述。
     * @param details 扩展错误详情。
     * @throws Exception 当消息发送失败时抛出。
     */
    private void sendErr(WebSocketSession session,
                         String cmd,
                         String id,
                         String reason,
                         String message,
                         Map<String, Object> details) throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("type", cmd + ".err");
        if (id != null) {
            root.put("id", id);
        }
        ObjectNode err = objectMapper.createObjectNode();
        err.put("reason", reason);
        err.put("message", message);
        if (details != null && !details.isEmpty()) {
            err.set("details", objectMapper.valueToTree(details));
        }
        root.set("error", err);
        send(session, root);
    }

    /**
     * 发送 JSON 文本消息。
     *
     * @param session 目标 WebSocket 会话。
     * @param json JSON 消息体。
     * @throws Exception 当消息发送失败时抛出。
     */
    private void send(WebSocketSession session, JsonNode json) throws Exception {
        if (!session.isOpen()) {
            return;
        }
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(json)));
    }

    /**
     * 将 JSON 节点转换为字符串。
     *
     * @param n JSON 节点。
     * @return 文本值；当节点为空返回 {@code null}。
     */
    private String text(JsonNode n) {
        if (n == null || n.isNull()) {
            return null;
        }
        if (n.isTextual()) {
            return n.asText();
        }
        return n.toString();
    }

    /**
     * 解析远端客户端 IP。
     *
     * @param session WebSocket 会话。
     * @return 远端 IP；无法解析时返回 {@code unknown}。
     */
    private String remoteIp(WebSocketSession session) {
        try {
            List<String> xff = session.getHandshakeHeaders().get("X-Forwarded-For");
            if (xff != null && !xff.isEmpty()) {
                return xff.getFirst();
            }
        } catch (Exception ignored) {
        }
        return "unknown";
    }
}
