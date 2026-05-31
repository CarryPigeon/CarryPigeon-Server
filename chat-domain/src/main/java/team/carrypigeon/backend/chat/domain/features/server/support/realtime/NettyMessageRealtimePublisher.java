package team.carrypigeon.backend.chat.domain.features.server.support.realtime;

import com.fasterxml.jackson.core.type.TypeReference;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelPin;
import org.springframework.context.annotation.Primary;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.Mention;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.MessageRealtimePublisher;
import team.carrypigeon.backend.chat.domain.features.message.support.payload.MessageAttachmentPayloadResolver;
import team.carrypigeon.backend.chat.domain.features.server.controller.ws.RealtimeServerMessage;
import team.carrypigeon.backend.chat.domain.features.user.domain.model.UserProfile;
import team.carrypigeon.backend.chat.domain.features.user.domain.repository.UserProfileRepository;
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
    private final MessageAttachmentPayloadResolver messageAttachmentPayloadResolver;
    private final UserProfileRepository userProfileRepository;

    public NettyMessageRealtimePublisher(
            RealtimeSessionRegistry realtimeSessionRegistry,
            JsonProvider jsonProvider,
            TimeProvider timeProvider,
            IdGenerator idGenerator,
            MessageAttachmentPayloadResolver messageAttachmentPayloadResolver,
            UserProfileRepository userProfileRepository
    ) {
        this.realtimeSessionRegistry = realtimeSessionRegistry;
        this.jsonProvider = jsonProvider;
        this.timeProvider = timeProvider;
        this.idGenerator = idGenerator;
        this.messageAttachmentPayloadResolver = messageAttachmentPayloadResolver;
        this.userProfileRepository = userProfileRepository;
    }

    @Override
    public void publish(ChannelMessage message, Collection<Long> recipientAccountIds) {
        publishEvent("message.created", createdPayload(message), recipientAccountIds);
    }

    @Override
    public void publishUpdate(ChannelMessage message, Collection<Long> recipientAccountIds) {
        if ("recalled".equals(message.status())) {
            publishEvent("message.deleted", Map.of(
                    "cid", Long.toString(message.channelId()),
                    "mid", Long.toString(message.messageId()),
                    "delete_time", timeProvider.nowMillis()
            ), recipientAccountIds);
            return;
        }
        publishEvent("message.updated", createdPayload(message), recipientAccountIds);
    }

    @Override
    public void publishPin(ChannelPin pin, Collection<Long> recipientAccountIds) {
        publishEvent("message.pinned", Map.of(
                "cid", Long.toString(pin.channelId()),
                "mid", Long.toString(pin.messageId()),
                "pin_id", Long.toString(pin.pinId()),
                "pinned_by_uid", Long.toString(pin.pinnedByAccountId()),
                "pinned_at", pin.pinnedAt().toEpochMilli()
        ), recipientAccountIds);
    }

    @Override
    public void publishUnpin(ChannelPin pin, long unpinnedByAccountId, long unpinnedAt, Collection<Long> recipientAccountIds) {
        publishEvent("message.unpinned", Map.of(
                "cid", Long.toString(pin.channelId()),
                "mid", Long.toString(pin.messageId()),
                "pin_id", Long.toString(pin.pinId()),
                "unpinned_by_uid", Long.toString(unpinnedByAccountId),
                "unpinned_at", unpinnedAt
        ), recipientAccountIds);
    }

    @Override
    public void publishMentionCreated(Mention mention, Collection<Long> recipientAccountIds) {
        publishEvent("mention.created", Map.of(
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

    private void publishEvent(String eventType, Object payload, Collection<Long> recipientAccountIds) {
        String eventId = idGenerator.nextStringId();
        long serverTime = timeProvider.nowMillis();
        realtimeSessionRegistry.appendEvent(new RealtimeSessionRegistry.StoredRealtimeEvent(eventId, eventType, serverTime, payload));
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
        for (Long recipientAccountId : recipientAccountIds) {
            realtimeSessionRegistry.getChannels(recipientAccountId)
                    .forEach(channel -> channel.writeAndFlush(new TextWebSocketFrame(frameText)));
        }
    }

    private Map<String, Object> createdPayload(ChannelMessage message) {
        UserProfile senderProfile = userProfileRepository.findByAccountId(message.senderId()).orElse(null);
        Map<String, Object> sender = new java.util.LinkedHashMap<>();
        sender.put("uid", Long.toString(message.senderId()));
        sender.put("nickname", senderProfile == null ? "" : senderProfile.nickname());
        sender.put("avatar", senderProfile == null ? "" : senderProfile.avatarUrl());

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

    private Object jsonField(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return null;
        }
        return jsonProvider.readTree(rawJson);
    }
}
