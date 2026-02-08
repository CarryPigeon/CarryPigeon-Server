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
 * API WebSocket 协议处理器（/api/ws）。
 * <p>
 * 协议约定见：{@code doc/api/12-WebSocket事件清单.md}。<br/>
 * 本实现采用“消息级鉴权”：连接建立后客户端必须先发送 {@code auth} 才会进入已登录态。
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

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("WS 连接建立：sessionId={}, ip={}", session.getId(), remoteIp(session));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessionRegistry.unregister(session);
        log.info("WS 连接关闭：sessionId={}, status={}", session.getId(), status);
    }

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
                // ignore unknown message types for forward compatibility
            }
        }
    }

    /**
     * 处理 {@code auth} 命令：绑定会话 uid 并可选执行 resume 回放。
     */
    private void handleAuth(WebSocketSession session, String id, JsonNode data) throws Exception {
        if (data == null || !data.isObject()) {
            sendErr(session, "auth", id, "validation_failed", "validation failed", null);
            return;
        }
        Integer apiVersion = data.has("api_version") && data.get("api_version").canConvertToInt()
                ? data.get("api_version").asInt()
                : null;
        if (apiVersion != null && apiVersion != 1) {
            sendErr(session, "auth", id, "api_version_unsupported", "api version unsupported", null);
            return;
        }

        String accessToken = text(data.get("access_token"));
        AccessTokenService.TokenInfo info = accessTokenService.verifyInfo(accessToken);
        if (info == null) {
            sendErr(session, "auth", id, "unauthorized", "missing or invalid access token", null);
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

        // resume (optional)
        JsonNode resume = data.get("resume");
        String lastEventId = resume != null && resume.isObject() ? text(resume.get("last_event_id")) : null;
        if (lastEventId != null && !lastEventId.isBlank()) {
            resumeOrFail(session, lastEventId);
        }
    }

    /**
     * 处理 {@code reauth} 命令：在同一连接上刷新 access_token（例如 HTTP refresh 后同步到 WS）。
     */
    private void handleReauth(WebSocketSession session, String id, JsonNode data) throws Exception {
        if (data == null || !data.isObject()) {
            sendErr(session, "reauth", id, "validation_failed", "validation failed", null);
            return;
        }
        String accessToken = text(data.get("access_token"));
        AccessTokenService.TokenInfo info = accessTokenService.verifyInfo(accessToken);
        if (info == null) {
            sendErr(session, "reauth", id, "unauthorized", "missing or invalid access token", null);
            return;
        }
        long uid = info.uid();

        Long oldUid = uidOf(session);
        if (oldUid == null || oldUid != uid) {
            // treat as re-bind
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
     * 处理 {@code subscribe} 命令：设置“频道级订阅过滤”（可选优化）。
     * <p>
     * 约定：
     * <ul>
     *   <li>未调用 subscribe：服务端按默认推送模型推送所有相关事件</li>
     *   <li>传空 cids：清除订阅过滤（恢复默认推送模型）</li>
     * </ul>
     */
    private void handleSubscribe(WebSocketSession session, String id, JsonNode data) throws Exception {
        // optional optimization; default behavior is "push everything relevant to current uid"
        if (uidOf(session) == null) {
            sendErr(session, "subscribe", id, "unauthorized", "unauthorized", null);
            return;
        }
        Set<Long> cids = Set.of();
        if (data != null && data.isObject() && data.has("cids") && data.get("cids").isArray()) {
            cids = streamTextArray(data.get("cids")).stream()
                    .map(this::parseLongOrNull)
                    .filter(v -> v != null && v > 0)
                    .collect(Collectors.toSet());
        }
        if (cids == null || cids.isEmpty()) {
            session.getAttributes().remove(ApiWsSessionAttributes.SUB_CIDS);
        } else {
            session.getAttributes().put(ApiWsSessionAttributes.SUB_CIDS, cids);
        }
        ObjectNode okData = objectMapper.createObjectNode();
        okData.set("cids", objectMapper.valueToTree(cids.stream().map(Object::toString).toList()));
        sendOk(session, "subscribe", id, okData);
    }

    /**
     * 尝试从事件存储中回放 {@code last_event_id} 之后的事件；若无法回放则通知客户端 {@code resume.failed}。
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
     * 从会话属性读取 uid。
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
     * 将字符串解析为 long；失败返回 null。
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
     * 将 JSON 数组中的元素按“文本”取出（非文本则用 {@link JsonNode#toString()} 兜底）。
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
     * 发送命令成功响应：{@code <cmd>.ok}。
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
     * 发送命令失败响应：{@code <cmd>.err}。
     */
    private void sendErr(WebSocketSession session, String cmd, String id, String reason, String message, Map<String, Object> details) throws Exception {
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
     * 发送 JSON 文本消息（会话关闭时忽略）。
     */
    private void send(WebSocketSession session, JsonNode json) throws Exception {
        if (!session.isOpen()) {
            return;
        }
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(json)));
    }

    /**
     * 将节点转换为字符串值。
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
     * 尝试解析远端 IP（优先 X-Forwarded-For）。
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
