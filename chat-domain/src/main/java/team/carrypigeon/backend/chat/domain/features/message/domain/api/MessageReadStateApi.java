package team.carrypigeon.backend.chat.domain.features.message.domain.api;

import java.util.List;
import team.carrypigeon.backend.chat.domain.features.message.domain.command.UpdateChannelReadStateCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.projection.ChannelReadStateResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.projection.ChannelUnreadResult;

/**
 * 消息读状态领域 API。
 * 职责：暴露频道消息已读位置更新与未读统计能力。
 * 边界：不暴露消息模型、读状态仓储或数据库统计实现。
 * 输入：当前账号、频道和最后已读消息位置。
 * 输出：更新后的读状态或各频道未读投影。
 * 失败语义：消息不存在、消息频道不匹配、非频道成员和参数非法由领域问题异常表达。
 * 调用方：HTTP controller 通过本接口访问读状态，不直接组合 message 与 channel 仓储。
 */
public interface MessageReadStateApi {

    /**
     * 更新当前账号在频道中的最后已读消息位置。
     *
     * @param command 读状态更新命令
     * @return 更新后或保持不变的读状态
     */
    ChannelReadStateResult updateChannelReadState(UpdateChannelReadStateCommand command);

    /**
     * 查询当前账号各频道的未读统计。
     *
     * @param accountId 当前账号 ID
     * @return 未读统计列表
     */
    List<ChannelUnreadResult> listUnreads(long accountId);
}
