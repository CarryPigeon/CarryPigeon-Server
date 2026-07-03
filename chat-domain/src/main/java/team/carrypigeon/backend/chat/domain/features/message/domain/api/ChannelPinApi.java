package team.carrypigeon.backend.chat.domain.features.message.domain.api;

import java.util.List;
import team.carrypigeon.backend.chat.domain.features.message.domain.command.PinChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.command.UnpinChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.projection.ChannelPinResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.query.ListChannelPinsQuery;

/**
 * 频道置顶领域 API。
 * 职责：暴露频道消息置顶、取消置顶和置顶列表能力。
 * 边界：不暴露普通消息发送、编辑、历史搜索、controller 协议和置顶存储细节。
 * 输入：置顶、取消置顶命令和置顶列表查询对象。
 * 输出：频道置顶投影列表或置顶副作用。
 * 失败语义：频道权限、消息不存在、置顶数量上限和重复状态由领域问题异常表达。
 * 调用方：通过本接口维护置顶语义，不直接写频道置顶存储。
 */
public interface ChannelPinApi {

    /**
     * 置顶频道消息。
     * 输入：命令携带操作者、频道、消息和可选备注。
     * 输出：创建后的频道置顶投影。
     * 副作用：写入置顶关系并发布相关频道事件。
     *
     * @param command 频道消息置顶业务命令
     * @return 创建后的频道置顶投影
     */
    ChannelPinResult pinChannelMessage(PinChannelMessageCommand command);

    /**
     * 取消频道消息置顶。
     * 输入：命令携带操作者、频道和消息。
     * 副作用：删除置顶关系并发布相关频道事件。
     * 约束：取消置顶权限由频道治理规则控制。
     *
     * @param command 取消频道消息置顶业务命令
     */
    void unpinChannelMessage(UnpinChannelMessageCommand command);

    /**
     * 查询频道置顶消息列表。
     * 输入：查询对象携带账号、频道、游标和数量。
     * 输出：频道置顶投影列表。
     * 约束：频道可访问性和分页边界由领域实现校验。
     *
     * @param query 频道置顶列表查询条件
     * @return 频道置顶投影列表
     */
    List<ChannelPinResult> listChannelPins(ListChannelPinsQuery query);
}
