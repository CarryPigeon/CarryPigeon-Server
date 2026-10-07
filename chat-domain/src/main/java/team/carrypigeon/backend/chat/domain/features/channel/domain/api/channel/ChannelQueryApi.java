package team.carrypigeon.backend.chat.domain.features.channel.domain.api.channel;

import java.util.List;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.channel.result.DiscoverChannelResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.channel.input.DiscoverChannelsQuery;

/**
 * 频道查询领域 API。
 * 职责：暴露频道发现查询能力。
 * 边界：不暴露 controller 协议、具体实现类和查询仓储细节。
 * 输入：频道查询对象或账号、频道标识等稳定业务入参。
 * 输出：频道发现投影列表。
 * 失败语义：频道不存在、成员权限不足和查询参数非法由领域问题异常表达。
 * 调用方：只消费领域投影，不依赖数据库记录或协议 DTO。
 */
public interface ChannelQueryApi {

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

}
