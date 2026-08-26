package team.carrypigeon.backend.chat.domain.features.channel.domain.api;

import java.util.List;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelMessagingContext;

/**
 * 频道上下文领域 API。
 * 职责：暴露频道存在性、成员关系和事件接收账号查询能力。
 * 边界：不暴露发送/撤回策略、置顶存储、审计仓储或 channel 模型。
 * 输入：频道与账号稳定标识。
 * 输出：最小频道上下文、成员判断或账号 ID 列表。
 * 失败语义：频道不存在和成员关系不足由领域问题异常表达。
 * 调用方：message、file 和 server feature 通过本接口读取频道上下文。
 */
public interface ChannelContextApi {

    ChannelMessagingContext requireChannel(long channelId);

    ChannelMessagingContext requireMemberChannel(long channelId, long accountId);

    boolean isMember(long channelId, long accountId);

    List<Long> recipientAccountIds(long channelId);
}
