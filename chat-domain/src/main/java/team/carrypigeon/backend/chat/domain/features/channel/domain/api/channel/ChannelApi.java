package team.carrypigeon.backend.chat.domain.features.channel.domain.api.channel;


import java.util.List;

import team.carrypigeon.backend.chat.domain.features.channel.domain.api.channel.result.ChannelResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.channel.input.CreateChannelCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.channel.input.DeleteChannelCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.channel.input.UpdateChannelProfileCommand;

/**
 * 频道核心领域 API。
 * 职责：暴露频道基础查询、创建、删除与资料更新能力。
 * 边界：不暴露 controller 协议、具体实现类和仓储细节。
 * 失败语义：权限不足、频道不存在、资料非法和状态冲突由领域问题异常表达。
 */
public interface ChannelApi {

    /**
     * 查询当前账号可见的频道列表。
     *
     * @param accountId 当前账号 ID
     * @return 可见频道投影列表
     */
    List<ChannelResult> listChannels(long accountId);

    /**
     * 查询当前账号可见的单个频道。
     *
     * @param accountId 当前账号 ID,用于进行可见性审核
     * @param channelId 频道 ID,用于查询频道信息
     * @return 频道投影
     */
    ChannelResult getChannelById(long accountId, long channelId);

    /**
     * 创建普通频道。
     * 输入：命令携带创建者和频道资料。
     * 输出：创建完成后的频道投影。
     * 约束：频道名称、可见性和创建权限由领域规则校验。
     * TODO 通过配置文件开关普通频道的创建
     * @param command 频道创建业务命令
     * @return 创建后的频道投影
     */
    ChannelResult createChannel(CreateChannelCommand command);

    /**
     * 删除频道。
     * 输入：命令携带操作者和目标频道。
     * 副作用：删除或失效目标频道，并影响成员、消息等后续访问语义。
     * 约束：只有具备频道生命周期管理权限的账号可以删除频道。
     *
     * @param command 频道删除业务命令
     */
    void deleteChannel(DeleteChannelCommand command);

    /**
     * 更新频道资料。
     * 输入：命令携带操作者、目标频道和待更新资料。
     * 输出：更新后的频道投影。
     * 约束：调用方不应直接更新频道模型，应通过本接口保留资料校验和审计语义。
     *
     * @param command 频道资料更新业务命令
     * @return 更新后的频道投影
     */
    ChannelResult updateChannelProfile(UpdateChannelProfileCommand command);
}
