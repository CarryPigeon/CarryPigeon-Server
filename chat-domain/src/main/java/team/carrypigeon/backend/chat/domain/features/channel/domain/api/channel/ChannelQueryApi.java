package team.carrypigeon.backend.chat.domain.features.channel.domain.api.channel;

import java.util.List;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.channel.result.DiscoverChannelResult;

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
     * 发现全部普通频道摘要，系统频道隐藏；结果不授予消息或频道详情的读取权限。
     *
     * @param accountId 账号标识
     * @param keyword 名称或简介的普通文本关键词，null 或空白表示不过滤
     * @param cursorChannelId 排他下界频道 ID，null 从头开始
     * @param limit 分页大小
     * @return 按频道 ID 升序排列，最多 limit + 1 条；末条为调用方判断下一页的探测数据
     */
    List<DiscoverChannelResult> discoverChannels(long accountId, String keyword, Long cursorChannelId, int limit);

}
