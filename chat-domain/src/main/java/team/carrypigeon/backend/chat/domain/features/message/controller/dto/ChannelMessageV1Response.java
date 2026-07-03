package team.carrypigeon.backend.chat.domain.features.message.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;

/**
 * v1 消息响应。
 */
public record ChannelMessageV1Response(
        @Schema(description = "消息 ID", example = "1")
        String mid,
        @Schema(description = "频道 ID", example = "12345")
        String cid,
        @Schema(description = "发送者用户 ID", example = "67890")
        String uid,
        @Schema(description = "发送者快照")
        MessageSenderResponse sender,
        @Schema(description = "发送时间（epoch 毫秒）", example = "1700000000000")
        long sendTime,
        @Schema(description = "最后编辑时间（epoch 毫秒）", example = "1700000100000")
        Long editedAt,
        @Schema(description = "编辑版本号", example = "2")
        long editVersion,
        @Schema(description = "消息 domain", example = "Core:Text")
        String domain,
        @Schema(description = "domain 版本", example = "1.0.0")
        String domainVersion,
        @Schema(description = "结构化消息数据")
        Map<String, Object> data,
        @Schema(description = "规范化提及列表")
        List<MentionTargetResponse> mentions,
        @Schema(description = "转发来源摘要")
        ForwardedFromResponse forwardedFrom,
        @Schema(description = "预览文本", example = "hello")
        String preview
) {

    public ChannelMessageV1Response(
            String mid,
            String cid,
            String uid,
            MessageSenderResponse sender,
            long sendTime,
            String domain,
            String domainVersion,
            Map<String, Object> data,
            String preview
    ) {
        this(mid, cid, uid, sender, sendTime, null, 1L, domain, domainVersion, data, List.of(), null, preview);
    }

    /**
     * v1 消息响应中的提及目标。
     * 职责：以协议字段表达消息中规范化后的 mention 目标。
     */
    public record MentionTargetResponse(
            @Schema(description = "提及目标类型", example = "user") String type,
            @Schema(description = "提及目标用户 ID", example = "67890") String uid
    ) {
    }

    /**
     * v1 消息响应中的转发来源摘要。
     * 职责：展示源消息、源频道和源发送者的稳定引用信息。
     */
    public record ForwardedFromResponse(
            @Schema(description = "源消息 ID", example = "723155640365318144") String mid,
            @Schema(description = "源频道 ID", example = "12345") String cid,
            @Schema(description = "源发送者 UID", example = "67890") String uid,
            @Schema(description = "源消息预览", example = "hello") String preview,
            @Schema(description = "源发送时间", example = "1700000000000") long sendTime
    ) {
    }
}
