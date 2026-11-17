package team.carrypigeon.backend.chat.domain.features.message.controller.http.dto;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.result.ChannelMessageResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.message.messages.text.CPChannelTextMessageData;
import team.carrypigeon.backend.infrastructure.basic.id.IdUtil;

/**
 * 消息响应。
 * 职责：把领域消息无损转换为统一 HTTP 消息结构。
 * 边界：domain 专属字段位于 data，数值 ID 在 HTTP 响应映射时转成字符串。
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
        Map<String, Object> responseData = result.data();
        if (CPChannelTextMessageData.DOMAIN.equals(result.domain()) && responseData.containsKey("reply_to_mid")) {
            responseData = new LinkedHashMap<>(responseData);
            Long replyId = (Long) responseData.get("reply_to_mid");
            responseData.put("reply_to_mid", replyId == null ? null : replyId.toString());
        }
        return new ChannelMessageResponse(
            IdUtil.toString(result.messageId()),
            IdUtil.toString(result.senderId()),
            IdUtil.toString(result.channelId()),
            result.domain(),
            responseData,
            result.preview(),
            result.mentions().stream().map(IdUtil::toString).toList(),
            result.sendTime().toEpochMilli()
        );
    }
}
