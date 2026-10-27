package team.carrypigeon.backend.chat.domain.features.channel.domain.api.channel;

import java.util.List;

import team.carrypigeon.backend.chat.domain.features.channel.domain.api.channel.result.ChannelResult;

/**
 * 频道核心领域 API。
 * 职责：暴露频道列表与详情读取、频道创建、删除与资料更新能力。
 * 边界：不暴露 controller 协议、具体实现类与仓储细节；不负责成员治理、禁言与入群申请。
 */
public interface ChannelApi {

    /**
     * 查询当前账号可见的频道列表。
     *
     * @param accountId 当前账号 ID
     * @return 可见频道投影列表；账号未加入任何频道时返回空列表
     */
    List<ChannelResult> listChannels(long accountId);

    /**
     * 查询当前账号可见的单个频道。
     *
     * @param accountId 当前账号 ID，用于可见性校验
     * @param channelId 频道 ID
     * @return 频道投影
     */
    ChannelResult getChannelById(long accountId, long channelId);

    /**
     * 创建普通频道并把创建者设为所有者。
     * TODO: 需要提供配置开关以在部署侧关闭普通频道创建能力
     *
     * @param accountId 创建者账号 ID
     * @param name 频道名称
     * @param brief 频道简介
     * @return 创建后的频道投影
     */
    ChannelResult createChannel(long accountId, String name, String brief);

    /**
     * 删除频道及其全部成员关系。
     *
     * @param operatorAccountId 操作者账号 ID
     * @param channelId 频道 ID
     */
    void deleteChannel(long operatorAccountId, long channelId);

    /**
     * 更新频道名称与简介。
     *
     * @param operatorAccountId 操作者账号 ID
     * @param channelId 频道 ID
     * @param name 频道名称
     * @param brief 频道简介
     */
    void updateChannelProfile(long operatorAccountId, long channelId, String name, String brief);
}
