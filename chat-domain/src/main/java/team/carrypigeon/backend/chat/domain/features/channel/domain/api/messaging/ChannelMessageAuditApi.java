package team.carrypigeon.backend.chat.domain.features.channel.domain.api.messaging;



import team.carrypigeon.backend.chat.domain.features.channel.domain.api.messaging.input.AppendMessageRecallAuditCommand;

/**
 * 频道消息审计 API。
 * 职责：追加由消息生命周期产生的频道审计事实。
 * 边界：不读取消息或频道模型，不执行权限校验，不触发 realtime 发布；审计内容只在追加时写入，不提供查询与修改。
 * 输入：包含审计记录 ID、频道、参与账号、消息与发生时间的撤回审计命令。
 * 输出：无返回值，成功即表示一条审计记录已写入。
 * 失败语义：持久化失败按基础设施异常边界传播，本接口不将其转换为业务失败。
 * 调用方：message feature 在撤回消息的同一事务内调用。
 */
public interface ChannelMessageAuditApi {

    /**
     * 追加一条消息撤回审计事实。
     * 副作用：写入一条频道审计记录；审计记录 ID 由调用方提供，本接口不生成主键。
     *
     * @param command 消息撤回审计追加命令
     */
    void appendMessageRecallAudit(AppendMessageRecallAuditCommand command);
}
