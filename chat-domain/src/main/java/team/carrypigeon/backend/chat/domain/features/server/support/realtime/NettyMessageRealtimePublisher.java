package team.carrypigeon.backend.chat.domain.features.server.support.realtime;

import com.fasterxml.jackson.core.type.TypeReference;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import org.springframework.context.annotation.Primary;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.Mention;
import team.carrypigeon.backend.chat.domain.features.message.domain.port.MessageChannelBoundary;
import team.carrypigeon.backend.chat.domain.features.message.domain.port.MessageRealtimePublisher;
import team.carrypigeon.backend.chat.domain.features.message.domain.port.MessageSenderSnapshot;
import team.carrypigeon.backend.chat.domain.features.message.domain.port.MessagePayloadResolver;
import team.carrypigeon.backend.chat.domain.features.server.controller.ws.RealtimeServerMessage;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;

/**
 * 基于 Netty 的消息实时发布器。
 * 职责：把已持久化业务消息转换为 v1 `event` envelope 并分发给在线成员通道。
 * 边界：只负责实时投递与最小事件日志记录，不参与消息鉴权与持久化规则。
 */
@Primary
public class NettyMessageRealtimePublisher implements MessageRealtimePublisher {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final RealtimeSessionRegistry realtimeSessionRegistry;
    private final JsonProvider jsonProvider;
    private final TimeProvider timeProvider;
    private final IdGenerator idGenerator;
    private final MessagePayloadResolver messageAttachmentPayloadResolver;
    private final RealtimeNotificationPreferenceFilter notificationPreferenceFilter;

    public NettyMessageRealtimePublisher(
            RealtimeSessionRegistry realtimeSessionRegistry,
            JsonProvider jsonProvider,
            TimeProvider timeProvider,
            IdGenerator idGenerator,
            MessagePayloadResolver messageAttachmentPayloadResolver
    ) {
        this(
                realtimeSessionRegistry,
                jsonProvider,
                timeProvider,
                idGenerator,
                messageAttachmentPayloadResolver,
                RealtimeNotificationPreferenceFilter.allowAll()
        );
    }

    public NettyMessageRealtimePublisher(
            RealtimeSessionRegistry realtimeSessionRegistry,
            JsonProvider jsonProvider,
            TimeProvider timeProvider,
            IdGenerator idGenerator,
            MessagePayloadResolver messageAttachmentPayloadResolver,
            RealtimeNotificationPreferenceFilter notificationPreferenceFilter
    ) {
        this.realtimeSessionRegistry = realtimeSessionRegistry;
        this.jsonProvider = jsonProvider;
        this.timeProvider = timeProvider;
        this.idGenerator = idGenerator;
        this.messageAttachmentPayloadResolver = messageAttachmentPayloadResolver;
        this.notificationPreferenceFilter = Objects.requireNonNull(notificationPreferenceFilter, "notificationPreferenceFilter");
    }

    /**
     * 广播一条新创建的消息事件。
     * 输入：已持久化消息与目标接收账户集合。
     * 副作用：会写入实时事件缓存并向在线会话推送 `message.created`。
     */
    @Override
    public void publish(ChannelMessage message, MessageSenderSnapshot senderSnapshot, Collection<Long> recipientAccountIds) {
        publishEvent(message.channelId(), "message.created", createdPayload(message, senderSnapshot), recipientAccountIds);
    }

    /**
     * 广播消息更新事件。
     * 约束：当消息状态已经变为 `recalled` 时，转换为 `message.recalled` 事件下发。
     */
    @Override
    public void publishUpdate(ChannelMessage message, MessageSenderSnapshot senderSnapshot, Collection<Long> recipientAccountIds) {
        if ("recalled".equals(message.status())) {
            publishEvent(message.channelId(), "message.recalled", Map.of(
                    "cid", Long.toString(message.channelId()),
                    "mid", Long.toString(message.messageId()),
                    "recall_time", timeProvider.nowMillis()
            ), recipientAccountIds);
            return;
        }
        publishEvent(message.channelId(), "message.updated", createdPayload(message, senderSnapshot), recipientAccountIds);
    }

    /**
     * 广播消息硬删除事件。
     * 输出：下发 `message.deleted`，仅表达消息从频道中消失。
     */
    @Override
    public void publishDelete(ChannelMessage message, Collection<Long> recipientAccountIds) {
        publishEvent(message.channelId(), "message.deleted", Map.of(
                "cid", Long.toString(message.channelId()),
                "mid", Long.toString(message.messageId()),
                "delete_time", timeProvider.nowMillis()
        ), recipientAccountIds);
    }

    /**
     * 广播置顶事件。
     * 输出：下发 `message.pinned` 事件，携带稳定 `pin_id` 与操作人信息。
     */
    @Override
    public void publishPin(MessageChannelBoundary.MessageChannelPin pin, Collection<Long> recipientAccountIds) {
        publishEvent(pin.channelId(), "message.pinned", Map.of(
                "cid", Long.toString(pin.channelId()),
                "mid", Long.toString(pin.messageId()),
                "pin_id", Long.toString(pin.pinId()),
                "pinned_by_uid", Long.toString(pin.pinnedByAccountId()),
                "pinned_at", pin.pinnedAt().toEpochMilli()
        ), recipientAccountIds);
    }

    /**
     * 广播取消置顶事件。
     * 输入：被取消的 pin、操作人和取消时间。
     * 副作用：向所有目标在线会话推送 `message.unpinned`。
     */
    @Override
    public void publishUnpin(MessageChannelBoundary.MessageChannelPin pin, long unpinnedByAccountId, long unpinnedAt, Collection<Long> recipientAccountIds) {
        publishEvent(pin.channelId(), "message.unpinned", Map.of(
                "cid", Long.toString(pin.channelId()),
                "mid", Long.toString(pin.messageId()),
                "pin_id", Long.toString(pin.pinId()),
                "unpinned_by_uid", Long.toString(unpinnedByAccountId),
                "unpinned_at", unpinnedAt
        ), recipientAccountIds);
    }

    /**
     * 广播新的提及事件。
     * 约束：事件体只暴露客户端消费所需的 mention 公开字段，不直接透传领域对象。
     */
    @Override
    public void publishMentionCreated(Mention mention, Collection<Long> recipientAccountIds) {
        publishEvent(mention.channelId(), "mention.created", Map.of(
                "mention_id", Long.toString(mention.mentionId()),
                "cid", Long.toString(mention.channelId()),
                "mid", Long.toString(mention.messageId()),
                "from_uid", Long.toString(mention.fromAccountId()),
                "target", Map.of(
                        "type", mention.targetType(),
                        "uid", Long.toString(mention.targetAccountId())
                ),
                "created_at", mention.createdAt().toEpochMilli()
        ), recipientAccountIds);
    }

    /**
     * 按通知偏好过滤后写入并推送消息实时事件。
     * 副作用：写入实时事件缓存，并向过滤后仍允许接收的在线会话推送 WebSocket frame。
     *
     * @param channelId 事件所属频道 ID
     * @param eventType realtime event 类型
     * @param payload 事件载荷
     * @param recipientAccountIds 候选接收账号集合
     */
    private void publishEvent(long channelId, String eventType, Object payload, Collection<Long> recipientAccountIds) {
        Collection<Long> filteredRecipientAccountIds = notificationPreferenceFilter.filterRecipients(channelId, eventType, recipientAccountIds);
        if (filteredRecipientAccountIds.isEmpty()) {
            return;
        }
        String eventId = idGenerator.nextStringId();
        long serverTime = timeProvider.nowMillis();
        realtimeSessionRegistry.appendEvent(RealtimeSessionRegistry.event(
                eventId,
                eventType,
                serverTime,
                payload,
                filteredRecipientAccountIds
        ));
        String frameText = jsonProvider.toJson(new RealtimeServerMessage(
                "event",
                null,
                Map.of(
                        "event_id", eventId,
                        "event_type", eventType,
                        "server_time", serverTime,
                        "payload", payload
                ),
                null
        ));
        for (Long recipientAccountId : filteredRecipientAccountIds) {
            realtimeSessionRegistry.getChannels(recipientAccountId)
                    .forEach(channel -> channel.writeAndFlush(new TextWebSocketFrame(frameText)));
        }
    }

    /**
     * 构造 `message.created` / `message.updated` 共用的 v1 消息载荷。
     * 约束：领域消息类型会转换为客户端稳定 domain，附件 payload 会先经过出站解析器收口。
     *
     * @param message 已持久化消息
     * @param senderSnapshot 发送者公开快照
     * @return v1 realtime 消息载荷
     */
    private Map<String, Object> createdPayload(ChannelMessage message, MessageSenderSnapshot senderSnapshot) {
        Map<String, Object> sender = new java.util.LinkedHashMap<>();
        sender.put("uid", Long.toString(senderSnapshot.accountId()));
        sender.put("nickname", senderSnapshot.nickname());
        sender.put("avatar", senderSnapshot.avatarUrl());

        Map<String, Object> messagePayload = new java.util.LinkedHashMap<>();
        messagePayload.put("mid", Long.toString(message.messageId()));
        messagePayload.put("cid", Long.toString(message.channelId()));
        messagePayload.put("uid", Long.toString(message.senderId()));
        messagePayload.put("sender", sender);
        messagePayload.put("send_time", message.createdAt().toEpochMilli());
        messagePayload.put("edited_at", message.editedAt() == null ? null : message.editedAt().toEpochMilli());
        messagePayload.put("edit_version", message.editVersion());
        messagePayload.put("domain", toDomain(message.messageType()));
        messagePayload.put("domain_version", "1.0.0");
        messagePayload.put("data", payloadData(message));
        messagePayload.put("mentions", jsonField(message.mentions()));
        messagePayload.put("forwarded_from", jsonField(message.forwardedFrom()));
        messagePayload.put("preview", message.previewText());

        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("cid", Long.toString(message.channelId()));
        payload.put("message", messagePayload);
        return payload;
    }

    /**
     * 解析 realtime 下发所需的消息 data 字段。
     * 语义：文本消息直接由正文生成 data，附件和扩展消息从 canonical payload 转为结构化对象。
     *
     * @param message 已持久化消息
     * @return 可序列化到 realtime payload 的 data 对象
     */
    private Object payloadData(ChannelMessage message) {
        if ("text".equals(message.messageType())) {
            return Map.of("text", message.body() == null ? "" : message.body());
        }
        if (message.payload() == null || message.payload().isBlank()) {
            return Map.of();
        }
        return jsonProvider.fromJson(
                messageAttachmentPayloadResolver.resolve(message.messageType(), message.payload()),
                MAP_TYPE
        );
    }

    private String toDomain(String messageType) {
        return switch (messageType) {
            case "text" -> "Core:Text";
            case "file" -> "Core:File";
            case "voice" -> "Core:Voice";
            default -> messageType;
        };
    }

    /**
     * 解析可选 JSON 字段供 realtime 载荷透出。
     * 约束：空白字段保持为 null，非空字段必须是已持久化的合法 JSON。
     *
     * @param rawJson 已持久化 JSON 文本
     * @return JSON 节点或 null
     */
    private Object jsonField(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return null;
        }
        return jsonProvider.readTree(rawJson);
    }
}
