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
 * 职责：统一把消息应用层结果转换为 docs/api/API.md 对齐的公共消息响应。
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
     * 将消息应用层结果映射为 docs/api/API.md 对齐的 v1 响应。
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

    /**
     * 构造 v1 HTTP 消息响应的 data 字段。
     * 语义：文本消息用正文生成稳定 data，非文本消息使用领域已解析后的 canonical payload。
     *
     * @param result 领域消息投影
     * @return v1 响应 data
     */
    private Map<String, Object> toData(ChannelMessageResult result) {
        if ("text".equals(result.messageType())) {
            return Map.of("text", result.body() == null ? "" : result.body());
        }
        if (result.payload() == null || result.payload().isBlank()) {
            return Map.of();
        }
        return jsonProvider.fromJson(result.payload(), MAP_TYPE);
    }

    /**
     * 把领域 mention JSON 转换为 v1 响应 mention 列表。
     * 约束：空白或非数组 mention 数据对外按空列表处理。
     *
     * @param mentionsJson 已持久化 mention JSON
     * @return v1 mention 响应列表
     */
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

    /**
     * 把领域转发来源 JSON 转换为 v1 响应对象。
     * 输出：无转发来源时返回 null，存在时只透出客户端展示所需字段。
     *
     * @param forwardedFromJson 已持久化转发来源 JSON
     * @return v1 转发来源响应
     */
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
