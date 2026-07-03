package team.carrypigeon.backend.chat.domain.features.channel.domain.api;

import java.util.List;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.AuditLogResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelBanListItemResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelMemberResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelUnreadResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.DiscoverChannelResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.query.DiscoverChannelsQuery;
import team.carrypigeon.backend.chat.domain.features.channel.domain.query.ListAuditLogsQuery;
import team.carrypigeon.backend.chat.domain.features.channel.domain.query.ListChannelBansQuery;
import team.carrypigeon.backend.chat.domain.features.channel.domain.query.ListChannelMembersQuery;

/**
 * 频道查询领域 API。
 * 职责：暴露频道列表、成员、封禁、审计、发现和未读查询能力。
 * 边界：不暴露 controller 协议、具体实现类和查询仓储细节。
 * 输入：频道查询对象或账号、频道标识等稳定业务入参。
 * 输出：频道、成员、封禁、审计、发现和未读投影列表。
 * 失败语义：频道不存在、成员权限不足和查询参数非法由领域问题异常表达。
 * 调用方：只消费领域投影，不依赖数据库记录或协议 DTO。
 */
public interface ChannelQueryApi {

    /**
     * 查询频道成员列表。
     * 输入：查询对象携带操作者、频道和分页或筛选条件。
     * 输出：频道成员投影列表。
     * 约束：成员可见性和 system 频道成员隐藏规则由领域实现控制。
     *
     * @param query 频道成员列表查询条件
     * @return 频道成员投影列表
     */
    List<ChannelMemberResult> listChannelMembers(ListChannelMembersQuery query);

    /**
     * 查询频道封禁列表。
     * 输入：查询对象携带操作者、频道和分页条件。
     * 输出：频道封禁列表项投影。
     * 约束：只有具备治理权限的账号可以读取封禁列表。
     *
     * @param query 频道封禁列表查询条件
     * @return 频道封禁列表项投影列表
     */
    List<ChannelBanListItemResult> listChannelBans(ListChannelBansQuery query);

    /**
     * 查询频道审计日志。
     * 输入：查询对象携带操作者、频道、动作类型和分页条件。
     * 输出：频道审计日志投影列表。
     * 约束：审计读取权限由频道治理规则控制。
     *
     * @param query 频道审计日志查询条件
     * @return 频道审计日志投影列表
     */
    List<AuditLogResult> listAuditLogs(ListAuditLogsQuery query);

    /**
     * 查询账号可访问的频道列表。
     * 输入：当前账号 ID。
     * 输出：该账号可见或已加入的频道投影列表。
     * 约束：列表范围由成员关系、频道可见性和系统频道规则决定。
     *
     * @param accountId 当前账号 ID
     * @return 当前账号可访问的频道投影列表
     */
    List<ChannelResult> listChannels(long accountId);

    /**
     * 按频道 ID 查询频道详情。
     * 输入：当前账号 ID 和目标频道 ID。
     * 输出：目标频道投影。
     * 失败语义：频道不存在或当前账号不可访问时返回领域问题。
     *
     * @param accountId 当前账号 ID
     * @param channelId 目标频道 ID
     * @return 目标频道投影
     */
    ChannelResult getChannelById(long accountId, long channelId);

    /**
     * 发现可加入或可浏览的频道。
     * 输入：发现查询包含当前账号、关键字和分页条件。
     * 输出：频道发现结果投影列表。
     * 约束：已封禁、不可见或不满足发现策略的频道不应出现在结果中。
     *
     * @param query 频道发现查询条件
     * @return 频道发现结果投影列表
     */
    List<DiscoverChannelResult> discoverChannels(DiscoverChannelsQuery query);

    /**
     * 查询账号在各频道的未读状态。
     * 输入：当前账号 ID。
     * 输出：频道未读计数和最后消息等未读投影列表。
     * 约束：未读计算基于领域读状态，不暴露具体统计实现。
     *
     * @param accountId 当前账号 ID
     * @return 当前账号的频道未读投影列表
     */
    List<ChannelUnreadResult> listUnreads(long accountId);
}
