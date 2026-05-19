package team.carrypigeon.backend.chat.domain.features.channel.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 频道所有权转移响应。
 * 职责：向 HTTP 协议层返回 OWNER 转移后的稳定角色结果。
 * 边界：只承载协议输出，不承载治理逻辑。
 *
 * @param channelId 频道 ID
 * @param previousOwnerAccountId 原 OWNER 账户 ID
 * @param previousOwnerRole 原 OWNER 转移后的角色
 * @param newOwnerAccountId 新 OWNER 账户 ID
 * @param newOwnerRole 新 OWNER 的角色
 */
public record ChannelOwnershipTransferResponse(
        @Schema(description = "频道 ID", example = "2001")
        long channelId,
        @Schema(description = "原 OWNER 账户 ID", example = "1001")
        long previousOwnerAccountId,
        @Schema(description = "原 OWNER 转移后的角色", example = "MEMBER")
        String previousOwnerRole,
        @Schema(description = "新 OWNER 账户 ID", example = "1002")
        long newOwnerAccountId,
        @Schema(description = "新 OWNER 角色", example = "OWNER")
        String newOwnerRole
) {
}
