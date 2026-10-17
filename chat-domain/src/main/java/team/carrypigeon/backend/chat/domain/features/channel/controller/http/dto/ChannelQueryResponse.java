package team.carrypigeon.backend.chat.domain.features.channel.controller.http.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 频道详情响应。
 * 职责：承载按频道 ID 查询资料所需的频道标识、名称、简介、系统频道标记与创建时间。
 * 边界：只表达频道聚合自身的字段，不含成员关系与禁言状态；创建时间已序列化为 epoch 毫秒。
 */
public record ChannelQueryResponse(
    @Schema(description = "频道 ID", example = "123456")
    String cid,
    @Schema(description = "频道名称", example = "技术交流")
    String name,
    @Schema(description = "频道简介", example = "一个关于技术交流的频道")
    String brief,
    @Schema(description = "是否为系统频道", example = "true")
    boolean systemChannel,
    @Schema(description = "创建时间（epoch 毫秒）", example = "1700000000000")
    Long createdAt
) {
}
