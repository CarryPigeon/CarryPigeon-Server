package team.carrypigeon.backend.chat.domain.features.channel.domain.api;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.AppendMessageRecallAuditCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.RequireMessageRecallPermissionCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelMessagingContext;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelPinReference;

/**
 * 频道消息协作 API。
 * 职责：向 message、file 和 server feature 暴露频道成员、发送权限、置顶和审计的最小能力。
 * 边界：不暴露 channel 聚合、仓储或治理策略实现。
 * 输入：频道、账号、消息引用、置顶和审计相关稳定业务参数。
 * 输出：频道上下文、成员账号、置顶引用或状态变更副作用。
 * 失败语义：频道不存在、成员权限不足、治理限制和资源冲突由领域问题异常表达。
 * 调用方：message、file、server 及本 feature controller 只能依赖本接口，不直接访问内部模型与仓储。
 */
public interface ChannelMessagingApi {

    /**
     * 读取必须存在的频道消息上下文。
     *
     * @param channelId 频道 ID
     * @return 消息用例所需的最小频道上下文
     */
    ChannelMessagingContext requireChannel(long channelId);

    /**
     * 校验账号频道成员关系并返回频道上下文。
     *
     * @param channelId 频道 ID
     * @param accountId 待校验账号 ID
     * @return 已通过成员校验的频道上下文
     */
    ChannelMessagingContext requireMemberChannel(long channelId, long accountId);

    /**
     * 校验账号当前可在频道发送消息并返回频道上下文。
     *
     * @param channelId 频道 ID
     * @param accountId 发送账号 ID
     * @param now 权限判断时间
     * @return 已通过发送权限校验的频道上下文
     */
    ChannelMessagingContext requireSendableChannel(long channelId, long accountId, Instant now);

    /**
     * 校验操作者能否撤回指定发送者的频道消息。
     *
     * @param command 消息撤回权限校验命令
     */
    void requireRecallPermission(RequireMessageRecallPermissionCommand command);

    /**
     * 校验操作者能否管理频道置顶消息。
     *
     * @param channelId 频道 ID
     * @param operatorAccountId 操作者账号 ID
     */
    void requirePinModerationPermission(long channelId, long operatorAccountId);

    /**
     * 判断账号是否为频道成员。
     *
     * @param channelId 频道 ID
     * @param accountId 账号 ID
     * @return 是频道成员时返回 true
     */
    boolean isMember(long channelId, long accountId);

    /**
     * 列出频道 realtime 事件候选接收账号。
     *
     * @param channelId 频道 ID
     * @return 当前频道成员账号 ID 列表
     */
    List<Long> recipientAccountIds(long channelId);

    /**
     * 查询频道内指定消息的置顶引用。
     *
     * @param channelId 频道 ID
     * @param messageId 消息 ID
     * @return 存在时返回置顶引用
     */
    Optional<ChannelPinReference> findPin(long channelId, long messageId);

    /**
     * 统计频道当前置顶数量。
     *
     * @param channelId 频道 ID
     * @return 置顶数量
     */
    long countPins(long channelId);

    /**
     * 保存频道消息置顶并返回稳定引用。
     * 副作用：新增一条频道置顶记录。
     *
     * @param pinId 置顶 ID
     * @param channelId 频道 ID
     * @param messageId 消息 ID
     * @param pinnedByAccountId 置顶操作者账号 ID
     * @param note 置顶说明
     * @param pinnedAt 置顶时间
     * @return 保存后的置顶引用
     */
    ChannelPinReference savePin(
            long pinId,
            long channelId,
            long messageId,
            long pinnedByAccountId,
            String note,
            Instant pinnedAt
    );

    /**
     * 删除频道内指定消息的置顶记录。
     *
     * @param channelId 频道 ID
     * @param messageId 消息 ID
     */
    void deletePin(long channelId, long messageId);

    /**
     * 删除指定消息关联的全部置顶记录。
     *
     * @param messageId 消息 ID
     */
    void deletePinsByMessageId(long messageId);

    /**
     * 按消息游标列出频道置顶引用。
     *
     * @param channelId 频道 ID
     * @param cursorMessageId 可空的消息游标
     * @param limit 返回数量上限
     * @return 稳定排序的置顶引用列表
     */
    List<ChannelPinReference> findPinsBefore(long channelId, Long cursorMessageId, int limit);

    /**
     * 追加消息撤回频道审计记录。
     *
     * @param command 消息撤回审计命令
     */
    void appendMessageRecallAudit(AppendMessageRecallAuditCommand command);
}
