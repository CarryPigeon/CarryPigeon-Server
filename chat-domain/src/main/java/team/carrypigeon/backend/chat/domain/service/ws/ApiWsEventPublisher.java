package team.carrypigeon.backend.chat.domain.service.ws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.bo.domain.channel.read.CPChannelReadState;
import team.carrypigeon.backend.api.bo.domain.message.CPMessage;
import team.carrypigeon.backend.api.dao.database.channel.member.ChannelMemberDao;
import team.carrypigeon.backend.common.id.IdUtil;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * WS 事件发布器。
 * <p>
 * 职责：
 * <ul>
 *   <li>将业务变更映射为 WS 事件（event_type + payload）</li>
 *   <li>为每个事件生成单调递增的 {@code event_id}（雪花 ID）</li>
 *   <li>写入 {@link ApiWsEventStore} 形成可回放窗口（resume）</li>
 *   <li>向同一 uid 的所有在线 WS 会话推送事件</li>
 * </ul>
 * <p>
 * 文档：{@code doc/api/12-WebSocket事件清单.md}
 */
@Slf4j
@Service
public class ApiWsEventPublisher {

    private final ObjectMapper objectMapper;
    private final ApiWsSessionRegistry sessionRegistry;
    private final ApiWsEventStore eventStore;
    private final ApiWsPayloadMapper payloadMapper;
    private final ChannelMemberDao channelMemberDao;

    public ApiWsEventPublisher(ObjectMapper objectMapper,
                               ApiWsSessionRegistry sessionRegistry,
                               ApiWsEventStore eventStore,
                               ApiWsPayloadMapper payloadMapper,
                               ChannelMemberDao channelMemberDao) {
        this.objectMapper = objectMapper;
        this.sessionRegistry = sessionRegistry;
        this.eventStore = eventStore;
        this.payloadMapper = payloadMapper;
        this.channelMemberDao = channelMemberDao;
    }

    /**
     * 推送 {@code message.created}。
     */
    public void publishMessageCreated(CPMessage message) {
        if (message == null) {
            return;
        }
        List<Long> uids = channelMemberUids(message.getCid());
        ObjectNode payload = payloadMapper.messageCreatedPayload(message);
        publishToUsers(uids, "message.created", payload);
    }

    /**
     * 推送 {@code message.deleted}。
     */
    public void publishMessageDeleted(long cid, long mid) {
        List<Long> uids = channelMemberUids(cid);
        ObjectNode payload = payloadMapper.messageDeletedPayload(cid, mid, System.currentTimeMillis());
        publishToUsers(uids, "message.deleted", payload);
    }

    /**
     * 推送 {@code read_state.updated}（同 uid 多会话同步）。
     */
    public void publishReadStateUpdated(CPChannelReadState state) {
        if (state == null) {
            return;
        }
        ObjectNode payload = payloadMapper.readStateUpdatedPayload(state);
        publishToUsers(List.of(state.getUid()), "read_state.updated", payload);
    }

    /**
     * 推送 {@code channels.changed}（提示客户端刷新频道列表）。
     */
    public void publishChannelsChanged(Collection<Long> uids) {
        publishToUsers(uids, "channels.changed", payloadMapper.channelsChangedPayload());
    }

    /**
     * 推送 {@code channel.changed}（提示客户端刷新频道视图）。
     */
    public void publishChannelChanged(long cid, Collection<Long> uids, String scope) {
        publishToUsers(uids, "channel.changed", payloadMapper.channelChangedPayload(cid, scope));
    }

    /**
     * 对指定频道的所有成员推送 {@code channel.changed}。
     */
    public void publishChannelChangedToChannelMembers(long cid, String scope) {
        List<Long> uids = channelMemberUids(cid);
        publishChannelChanged(cid, uids, scope);
    }

    /**
     * 对指定频道的所有成员推送 {@code channels.changed}。
     */
    public void publishChannelsChangedToChannelMembers(long cid) {
        publishChannelsChanged(channelMemberUids(cid));
    }

    /**
     * 向指定用户集合推送事件。
     * <p>
     * 注意：
     * <ul>
     *   <li>该方法会写入事件存储，用于断线回放</li>
     *   <li>若 WS 会话设置了频道订阅过滤，将按 cid 做发送过滤</li>
     * </ul>
     */
    public void publishToUsers(Collection<Long> uids, String eventType, JsonNode payload) {
        if (uids == null || uids.isEmpty()) {
            return;
        }
        long eventIdLong = IdUtil.generateId();
        String eventId = Long.toString(eventIdLong);
        long serverTime = System.currentTimeMillis();

        ApiWsEventStore.StoredEvent stored = new ApiWsEventStore.StoredEvent(eventId, eventIdLong, eventType, serverTime, payload);
        eventStore.append(stored);

        ObjectNode envelope = toEventEnvelope(stored);
        String json;
        try {
            json = objectMapper.writeValueAsString(envelope);
        } catch (Exception e) {
            log.warn("WS 推送序列化失败：eventType={}", eventType, e);
            return;
        }
        TextMessage msg = new TextMessage(json);
        for (Long uid : uids) {
            if (uid == null || uid <= 0) {
                continue;
            }
            for (WebSocketSession s : sessionRegistry.sessionsOf(uid)) {
                try {
                    if (s.isOpen() && shouldSendToSession(s, stored)) {
                        s.sendMessage(msg);
                    }
                } catch (Exception e) {
                    log.debug("WS 推送发送失败：uid={}, sessionId={}, eventType={}", uid, s.getId(), eventType, e);
                }
            }
        }
    }

    public ObjectNode toEventEnvelope(ApiWsEventStore.StoredEvent e) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("type", "event");
        ObjectNode data = objectMapper.createObjectNode();
        data.put("event_id", e.eventId());
        data.put("event_type", e.eventType());
        data.put("server_time", e.serverTime());
        data.set("payload", e.payload() == null ? objectMapper.createObjectNode() : e.payload());
        root.set("data", data);
        return root;
    }

    /**
     * 判断事件是否应发送到指定 WS 会话。
     * <p>
     * 若会话未设置订阅过滤（未调用 subscribe 或已清除）：默认发送所有与该 uid 相关的事件。<br/>
     * 若会话设置了订阅 cid 集合：只发送 cid 命中的频道事件。
     */
    private boolean shouldSendToSession(WebSocketSession session, ApiWsEventStore.StoredEvent event) {
        if (session == null || event == null) {
            return false;
        }
        Object sub = session.getAttributes().get(ApiWsSessionAttributes.SUB_CIDS);
        if (sub == null) {
            return true;
        }

        Long cid = cidOf(event.payload());
        if (cid == null || cid <= 0) {
            // 非频道事件（或 payload 未包含 cid），不进行过滤
            return true;
        }

        if (sub instanceof java.util.Set<?> set) {
            return set.contains(cid);
        }
        if (sub instanceof java.util.Collection<?> c) {
            for (Object v : c) {
                if (v instanceof Long l && l.equals(cid)) {
                    return true;
                }
                if (v instanceof Integer i && cid.equals(i.longValue())) {
                    return true;
                }
                if (v instanceof String s) {
                    try {
                        if (cid.equals(Long.parseLong(s))) {
                            return true;
                        }
                    } catch (Exception ignored) {
                        // ignore
                    }
                }
            }
            return false;
        }
        return true;
    }

    /**
     * 从 payload 中解析 cid（按字符串或数字解析）。
     */
    private Long cidOf(JsonNode payload) {
        if (payload == null || !payload.isObject() || !payload.has("cid")) {
            return null;
        }
        JsonNode n = payload.get("cid");
        if (n == null || n.isNull()) {
            return null;
        }
        if (n.canConvertToLong()) {
            return n.asLong();
        }
        if (n.isTextual()) {
            try {
                return Long.parseLong(n.asText());
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }

    /**
     * 查询频道成员 uid 列表（去重）。
     */
    private List<Long> channelMemberUids(long cid) {
        CPChannelMember[] members = channelMemberDao.getAllMember(cid);
        if (members == null || members.length == 0) {
            return List.of();
        }
        return java.util.Arrays.stream(members)
                .filter(Objects::nonNull)
                .map(CPChannelMember::getUid)
                .distinct()
                .toList();
    }
}
