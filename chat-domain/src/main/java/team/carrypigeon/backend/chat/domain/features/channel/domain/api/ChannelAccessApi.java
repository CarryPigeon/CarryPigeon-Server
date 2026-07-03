package team.carrypigeon.backend.chat.domain.features.channel.domain.api;

import team.carrypigeon.backend.chat.domain.features.channel.domain.command.GetDefaultChannelCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.GetSystemChannelCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.UpdateChannelReadStateCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelReadStateResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelResult;

/**
 * 频道访问领域 API。
 * 职责：暴露默认频道、system 频道与读状态写入能力。
 * 边界：不暴露 controller 协议、具体实现类和仓储细节。
 * 输入：频道访问命令或账号、频道读取位置等业务入参。
 * 输出：频道投影或频道读状态投影。
 * 失败语义：成员身份、频道不存在或读状态参数非法由领域问题异常表达。
 * 调用方：controller、server 或其它 feature 通过本接口访问频道入口能力。
 */
public interface ChannelAccessApi {

    /**
     * 获取调用账号可进入的默认频道。
     * 输入：命令携带当前账号上下文。
     * 输出：默认频道的领域投影。
     * 约束：默认频道选择策略由领域实现负责，调用方不应硬编码频道 ID。
     *
     * @param command 默认频道读取业务命令
     * @return 当前账号可访问的默认频道投影
     */
    ChannelResult getDefaultChannel(GetDefaultChannelCommand command);

    /**
     * 获取调用账号可进入的 system 频道。
     * 输入：命令携带当前账号上下文。
     * 输出：system 频道的领域投影。
     * 约束：system 频道访问必须满足领域成员和频道类型规则。
     *
     * @param command system 频道读取业务命令
     * @return 当前账号可访问的 system 频道投影
     */
    ChannelResult getSystemChannel(GetSystemChannelCommand command);

    /**
     * 更新调用账号在频道内的消息已读位置。
     * 输入：命令携带账号、频道和已读消息游标。
     * 输出：更新后的频道读状态投影。
     * 副作用：持久化读状态并影响后续未读统计。
     *
     * @param command 频道读状态更新业务命令
     * @return 更新后的频道读状态投影
     */
    ChannelReadStateResult updateChannelReadState(UpdateChannelReadStateCommand command);
}
