package team.carrypigeon.backend.chat.domain.features.channel.domain.api.channel;

import java.util.List;

import team.carrypigeon.backend.chat.domain.features.channel.domain.api.channel.result.ChannelResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.channel.input.CreateChannelCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.channel.input.DeleteChannelCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.channel.input.UpdateChannelProfileCommand;

/**
 * 频道核心领域 API。
 * 职责：暴露频道列表与详情读取、频道创建、删除与资料更新能力。
 * 边界：不暴露 controller 协议、具体实现类与仓储细节；不负责成员治理、禁言与入群申请。
 * 输入：频道创建、删除与资料更新命令，以及账号与频道标识。
 * 输出：频道投影或频道投影列表。
 * 失败语义：频道不存在抛资源不存在；权限不足抛权限不足；资料非法抛参数错误；存在依赖数据时抛冲突。
 * 调用方：channel controller 与其它 feature 通过本接口操作频道，不直接依赖具体实现类。
 */
public interface ChannelApi {

    /**
     * 查询当前账号可见的频道列表。
     * 约束：可见性以成员关系为唯一标准，账号未加入的频道不会出现在结果中。
     *
     * @param accountId 当前账号 ID
     * @return 可见频道投影列表；账号未加入任何频道时返回空列表
     */
    List<ChannelResult> listChannels(long accountId);

    /**
     * 查询当前账号可见的单个频道。
     * 约束：账号必须是该频道成员，非成员一律拒绝。
     *
     * @param accountId 当前账号 ID，用于可见性校验
     * @param channelId 频道 ID
     * @return 频道投影
     */
    ChannelResult getChannelById(long accountId, long channelId);

    /**
     * 创建普通频道并把创建者设为所有者。
     * 输入：命令携带创建者账号与频道名称、简介、头像。
     * 输出：创建完成后的频道投影。
     * 副作用：写入频道记录与创建者的 OWNER 成员关系，并在事务提交后通知该账号刷新频道集合。
     * 约束：创建的始终是普通频道；系统频道不通过本接口产生。
     * TODO: 需要提供配置开关以在部署侧关闭普通频道创建能力
     *
     * @param command 频道创建业务命令
     * @return 创建后的频道投影
     */
    ChannelResult createChannel(CreateChannelCommand command);

    /**
     * 删除频道及其全部成员关系。
     * 输入：命令携带操作者与目标频道。
     * 副作用：删除频道记录与频道内全部成员关系，并在事务提交后通知操作者刷新频道集合。
     * 约束：仅频道所有者可删除；频道仍存在申请或禁言记录时拒绝删除。
     *
     * @param command 频道删除业务命令
     */
    void deleteChannel(DeleteChannelCommand command);

    /**
     * 更新频道名称与简介。
     * 输入：命令携带操作者、目标频道与新的名称、简介。
     * 输出：更新后的频道投影。
     * 副作用：覆盖频道名称与简介，并在事务提交后向频道全体成员发出资料变化通知。
     * 约束：调用方必须具备该频道 OWNER 或 ADMIN 成员角色；头像与创建时间不在更新范围内。
     *
     * @param command 频道资料更新业务命令
     * @return 更新后的频道投影
     */
    ChannelResult updateChannelProfile(UpdateChannelProfileCommand command);
}
