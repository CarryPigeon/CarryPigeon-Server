package team.carrypigeon.backend.chat.domain.features.channel.domain.api.channel;

import team.carrypigeon.backend.chat.domain.features.channel.domain.api.channel.result.ChannelResult;
import team.carrypigeon.backend.infrastructure.basic.page.PageQuery;
import team.carrypigeon.backend.infrastructure.basic.page.PageResult;

import java.util.List;

/**
 * 频道核心领域 API。
 * 职责：暴露频道列表与详情读取、频道创建、删除与资料更新能力。
 * 边界：不暴露 controller 协议、具体实现类与仓储细节；不负责成员治理、禁言与入群申请。
 */
public interface ChannelApi {

    /**
     * 分页查询当前账号可见的频道列表。
     *
     * @param accountId 当前账号 ID
     * @param page 分页参数
     * @return 分页频道投影；账号未加入任何频道时返回空页
     */
    PageResult<ChannelResult> listChannels(long accountId, PageQuery page);

    /**
     * 查询当前账号可见的单个频道。
     *
     * @param channelId 频道 ID
     * @return 频道投影
     */
    ChannelResult getChannelById(long channelId);

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
     * 撤下频道，提交后不可访问；关联数据交由进程内队列异步删除。
     * 频道不存在时返回不存在错误；清理任务不提供进程退出后的恢复保证。
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

    /**
     * 根据 ID 列表获取频道列表信息
     * @param channelIds 频道 ID 列表
     * @return 频道列表
     */
    List<ChannelResult> getChannelByIds(List<Long> channelIds);
}
