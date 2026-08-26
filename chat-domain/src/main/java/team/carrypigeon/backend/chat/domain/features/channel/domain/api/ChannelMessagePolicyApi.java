package team.carrypigeon.backend.chat.domain.features.channel.domain.api;

import java.time.Instant;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.RequireMessageRecallPermissionCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelMessagingContext;

/**
 * 频道消息治理策略 API。
 * 职责：暴露消息发送和撤回所需的频道权限判定。
 * 边界：不提供普通成员查询、置顶存储或审计写入。
 * 输入：频道、操作者、消息发送者和判定时间。
 * 输出：通过发送校验的频道上下文，或完成撤回权限校验。
 * 失败语义：频道不存在、非成员、禁言和治理权限不足由领域问题异常表达。
 * 调用方：message feature 在发送、上传附件或撤回消息前调用。
 */
public interface ChannelMessagePolicyApi {

    ChannelMessagingContext requireSendableChannel(long channelId, long accountId, Instant now);

    void requireRecallPermission(RequireMessageRecallPermissionCommand command);
}
