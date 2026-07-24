package team.carrypigeon.backend.chat.domain.features.channel.domain.api;

import team.carrypigeon.backend.chat.domain.features.channel.domain.command.InitializeChannelMembershipsCommand;

/**
 * 新账号频道成员关系初始化 API。
 * 职责：为新建账号建立默认频道与 system 频道成员关系。
 * 边界：不创建鉴权账号或用户资料。
 * 输入：包含新账号 ID 与初始化时间的成员关系初始化命令。
 * 输出：无返回值，副作用为保存基础频道成员关系。
 * 失败语义：基础频道缺失或成员关系初始化失败时由领域问题异常表达。
 * 调用方：auth 注册编排只能依赖本接口，不直接访问 channel 仓储或模型。
 */
public interface ChannelAccountProvisioningApi {

    /**
     * 初始化新账号的基础频道成员关系。
     * 副作用：为账号保存默认频道与 system 频道成员关系。
     *
     * @param command 成员关系初始化命令
     */
    void initializeMemberships(InitializeChannelMembershipsCommand command);
}
