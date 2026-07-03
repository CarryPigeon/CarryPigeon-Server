package team.carrypigeon.backend.chat.domain.features.message.controller.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import team.carrypigeon.backend.chat.domain.features.message.domain.projection.ChannelMessageResult;
import team.carrypigeon.backend.chat.domain.features.message.controller.dto.ChannelMessageV1Response;
import team.carrypigeon.backend.chat.domain.features.message.controller.dto.MessageSenderResponse;
import team.carrypigeon.backend.chat.domain.features.user.domain.command.GetUserProfileByAccountIdCommand;
import team.carrypigeon.backend.chat.domain.features.user.domain.projection.UserProfileResult;
import team.carrypigeon.backend.chat.domain.features.user.domain.api.UserProfileApi;
import team.carrypigeon.backend.infrastructure.basic.id.Ids;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;
import org.springframework.stereotype.Component;

/**
 * v1 消息响应映射器。
 * 职责：统一把消息应用层结果转换为 docs/t 对齐的公共消息响应。
 * 边界：只负责 HTTP 出站映射，不承载消息业务校验。
 */
@Component
public class ChannelMessageV1ResponseMapper {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final UserProfileApi userProfileDomainApi;
    private final JsonProvider jsonProvider;

    public ChannelMessageV1ResponseMapper(
            UserProfileApi userProfileDomainApi,
            JsonProvider jsonProvider
    ) {
        this.userProfileDomainApi = userProfileDomainApi;
        this.jsonProvider = jsonProvider;
    }

    /**
     * 将消息应用层结果映射为 docs/t 对齐的 v1 响应。
     * 输入：应用层消息结果快照。
     * 输出：带发送者、mentions、转发来源与 data 结构的稳定协议响应。
     *
     * @param result 应用层消息结果
     * @return v1 消息响应
     */
    public ChannelMessageV1Response toResponse(ChannelMessageResult result) {
        UserProfileResult sender = userProfileDomainApi == null ? null
                : userProfileDomainApi.getUserProfileByAccountId(new GetUserProfileByAccountIdCommand(result.senderId()));
        return new ChannelMessageV1Response(
                Ids.toString(result.messageId()),
                Ids.toString(result.channelId()),
                Ids.toString(result.senderId()),
                sender == null ? null : new MessageSenderResponse(Ids.toString(sender.accountId()), sender.nickname(), sender.avatarUrl()),
                result.createdAt().toEpochMilli(),
                result.editedAt() == null ? null : result.editedAt().toEpochMilli(),
                result.editVersion(),
                toDomain(result.messageType()),
                "1.0.0",
                toData(result),
                toMentionResponses(result.mentions()),
                toForwardedFromResponse(result.forwardedFrom()),
                result.previewText()
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

    private Map<String, Object> toData(ChannelMessageResult result) {
        if ("text".equals(result.messageType())) {
            return Map.of("text", result.body() == null ? "" : result.body());
        }
        if (result.payload() == null || result.payload().isBlank()) {
            return Map.of();
        }
        return jsonProvider.fromJson(result.payload(), MAP_TYPE);
    }

    private List<ChannelMessageV1Response.MentionTargetResponse> toMentionResponses(String mentionsJson) {
        if (mentionsJson == null || mentionsJson.isBlank()) {
            return List.of();
        }
        JsonNode mentionsNode = jsonProvider.readTree(mentionsJson);
        if (!mentionsNode.isArray()) {
            return List.of();
        }
        List<ChannelMessageV1Response.MentionTargetResponse> items = new ArrayList<>();
        mentionsNode.forEach(node -> items.add(new ChannelMessageV1Response.MentionTargetResponse(
                node.path("type").asText(),
                node.path("uid").asText()
        )));
        return items;
    }

    private ChannelMessageV1Response.ForwardedFromResponse toForwardedFromResponse(String forwardedFromJson) {
        if (forwardedFromJson == null || forwardedFromJson.isBlank()) {
            return null;
        }
        JsonNode node = jsonProvider.readTree(forwardedFromJson);
        return new ChannelMessageV1Response.ForwardedFromResponse(
                node.path("mid").asText(),
                node.path("cid").asText(),
                node.path("uid").asText(),
                node.path("preview").asText(),
                node.path("send_time").asLong()
        );
    }
}
