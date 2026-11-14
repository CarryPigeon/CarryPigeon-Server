package team.carrypigeon.backend.chat.domain.features.message.controller.http.dto;

import java.util.List;
import java.util.Map;

import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.result.ChannelMessageResult;
import team.carrypigeon.backend.infrastructure.basic.id.IdUtil;

/**
 * 消息响应。
 * 职责：把领域消息无损转换为统一 HTTP 消息结构。
 * 边界：只包含统一消息字段，domain 专属字段全部位于 data；不解析或重建 data。
 */
public record ChannelMessageResponse(
    String mid,
    String uid,
    String cid,
    String domain,
    Map<String,Object> data,
    String preview,
    List<String> mentions,
    Long sendTime
) {

    /**
     *
     * @param result canonical 消息结果
     * @return v1 消息响应
     */
    public static ChannelMessageResponse from(ChannelMessageResult result) {
        return new ChannelMessageResponse(
            IdUtil.toString(result.messageId()),
            IdUtil.toString(result.senderId()),
            IdUtil.toString(result.channelId()),
            result.domain(),
            result.data(),
            result.preview(),
            result.mentions().stream().map(IdUtil::toString).toList(),
            result.sendTime().toEpochMilli()
        );
    }
}
