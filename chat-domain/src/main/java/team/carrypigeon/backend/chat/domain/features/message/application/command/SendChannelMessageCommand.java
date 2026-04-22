package team.carrypigeon.backend.chat.domain.features.message.application.command;

import team.carrypigeon.backend.chat.domain.features.message.application.draft.ChannelMessageDraft;

/**
 * 通用频道消息发送命令。
 * 职责：表达已认证主体发送任意受支持消息草稿的最小输入。
 * 边界：命令本身只承载发送上下文与消息草稿，不展开插件内部规则。
 *
 * @param accountId 发送者账户 ID
 * @param channelId 目标频道 ID
 * @param draft 待发送消息草稿
 */
public record SendChannelMessageCommand(long accountId, long channelId, ChannelMessageDraft draft) {
}
