package team.carrypigeon.backend.chat.domain.features.channel.domain.api;

import team.carrypigeon.backend.chat.domain.features.channel.domain.command.AppendMessageRecallAuditCommand;

/**
 * 频道消息审计 API。
 * 职责：追加由消息生命周期产生的频道审计事实。
 * 边界：不读取消息或频道模型，不执行权限校验和 realtime 发布。
 * 输入：包含稳定 ID、参与账号和发生时间的撤回审计命令。
 * 输出：产生一条频道审计记录写入副作用。
 * 失败语义：持久化失败按基础设施异常边界传播。
 * 调用方：message feature 在撤回消息事务内调用。
 */
public interface ChannelMessageAuditApi {

    void appendMessageRecallAudit(AppendMessageRecallAuditCommand command);
}
