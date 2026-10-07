package team.carrypigeon.backend.chat.domain.features.channel.domain.api.context;



import java.util.List;
import java.time.Instant;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.context.input.RequireMessageRecallPermissionCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.context.result.ChannelMessagingContext;

/**
 * 频道上下文领域 API。
 * 职责：为跨 feature 提供最小频道上下文，以及消息发送和撤回前的频道策略检查。
 * 边界：不暴露频道查询、成员治理、置顶存储、审计仓储或 channel 模型。
 * 输入：频道与账号稳定标识。
 * 输出：最小频道上下文、成员判断、账号 ID 列表或策略检查副作用。
 * 失败语义：频道不存在和成员关系不足由领域问题异常表达。
 * 调用方：message、file 和 server feature 通过本接口读取频道上下文。
 */
public interface ChannelContextApi {

    ChannelMessagingContext requireChannel(long channelId);

    ChannelMessagingContext requireMemberChannel(long channelId, long accountId);

    boolean isMember(long channelId, long accountId);

    List<Long> recipientAccountIds(long channelId);

    ChannelMessagingContext requireSendableChannel(long channelId, long accountId, Instant now);

    void requireRecallPermission(RequireMessageRecallPermissionCommand command);
}
