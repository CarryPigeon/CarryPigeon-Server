package team.carrypigeon.backend.chat.domain.features.channel.domain.api;

import java.util.List;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.RemoveChannelPinCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.SetChannelPinCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelPinReference;
import team.carrypigeon.backend.chat.domain.features.channel.domain.query.ListChannelPinReferencesQuery;

/**
 * 频道消息置顶管理领域 API。
 * 职责：暴露 channel 拥有的置顶创建或替换、取消和成员列表读取能力。
 * 边界：不读取 message 模型，不暴露置顶仓储的查询、统计和写入步骤。
 * 输入：置顶业务命令或成员列表查询。
 * 输出：稳定置顶引用或引用列表。
 * 失败语义：频道不存在、成员或治理权限不足、置顶不存在和数量超限由领域问题异常表达。
 * 调用方：message feature 的消息置顶用例通过本接口管理 channel 拥有的置顶记录。
 */
public interface ChannelPinManagementApi {

    /**
     * 创建置顶，或替换同一频道消息的既有置顶。
     * 约束：操作者必须具备治理权限，频道最多保留 50 条置顶。
     *
     * @param command 设置频道置顶命令
     * @return 创建或替换后的置顶引用
     */
    ChannelPinReference setPin(SetChannelPinCommand command);

    /**
     * 取消指定频道消息的置顶。
     * 约束：操作者必须具备治理权限，目标置顶必须存在。
     *
     * @param command 取消频道置顶命令
     * @return 删除前的置顶引用
     */
    ChannelPinReference removePin(RemoveChannelPinCommand command);

    /**
     * 查询频道成员可见的置顶引用。
     *
     * @param query 频道置顶列表查询
     * @return 按消息游标倒序排列的置顶引用
     */
    List<ChannelPinReference> listPins(ListChannelPinReferencesQuery query);
}
