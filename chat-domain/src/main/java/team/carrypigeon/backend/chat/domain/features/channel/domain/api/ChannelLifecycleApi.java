package team.carrypigeon.backend.chat.domain.features.channel.domain.api;

import team.carrypigeon.backend.chat.domain.features.channel.domain.command.CreateChannelCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.CreatePrivateChannelCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.DeleteChannelCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.UpdateChannelProfileCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelResult;

/**
 * 频道生命周期领域 API。
 * 职责：暴露频道创建、删除与资料更新能力。
 * 边界：不暴露 controller 协议、具体实现类和仓储细节。
 * 输入：频道创建、删除和资料更新命令对象。
 * 输出：频道投影或删除副作用。
 * 失败语义：权限不足、频道不存在、资料非法和状态冲突由领域问题异常表达。
 * 调用方：controller 或其它 feature 通过本接口触发频道生命周期变化。
 */
public interface ChannelLifecycleApi {

    /**
     * 创建私有频道。
     * 输入：命令携带创建者、频道基础资料和初始成员信息。
     * 输出：创建完成后的私有频道投影。
     * 副作用：创建频道实体并建立创建者成员关系。
     *
     * @param command 私有频道创建业务命令
     * @return 创建后的私有频道投影
     */
    ChannelResult createPrivateChannel(CreatePrivateChannelCommand command);

    /**
     * 创建普通频道。
     * 输入：命令携带创建者和频道资料。
     * 输出：创建完成后的频道投影。
     * 约束：频道名称、可见性和创建权限由领域规则校验。
     *
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
