package team.carrypigeon.backend.chat.domain.features.message.domain.api;

import team.carrypigeon.backend.chat.domain.features.message.domain.command.ForwardChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.command.SendChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.command.SendSystemChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.projection.ChannelMessageResult;

/**
 * 频道消息发布领域 API。
 * 职责：暴露 canonical 频道消息创建、系统消息和消息转发能力。
 * 边界：不暴露编辑、撤回、删除、历史查询、搜索、附件上传和置顶能力。
 * 输入：canonical 消息发送、系统消息和转发命令对象。
 * 输出：创建后的频道消息投影。
 * 失败语义：频道权限、消息类型、领域版本、附件引用和消息源不存在等问题由领域问题异常表达。
 * 调用方：controller、realtime 入站处理器或其它 feature 通过本接口创建频道消息。
 */
public interface ChannelMessagePublishingApi {

    /**
     * 按 canonical envelope 发送频道消息。
     * 输入：命令携带发送账号、目标频道、domain、domain_version、data 和 mentions。
     * 输出：持久化后的频道消息投影。
     * 副作用：保存消息、持久化 mention，并在事务提交后发布消息创建事件。
     *
     * @param command 频道消息发送业务命令
     * @return 创建后的频道消息投影
     */
    ChannelMessageResult sendChannelMessage(SendChannelMessageCommand command);

    /**
     * 发送系统频道消息。
     * 输入：命令携带操作者、目标 system 频道、domain_version、data 和 mentions。
     * 输出：创建后的系统消息投影。
     * 约束：目标频道必须是 system 频道，消息类型由系统消息插件创建。
     *
     * @param command 系统频道消息发送业务命令
     * @return 创建后的系统消息投影
     */
    ChannelMessageResult sendSystemChannelMessage(SendSystemChannelMessageCommand command);

    /**
     * 转发已有频道消息到目标频道。
     * 输入：命令携带转发账号、源消息、目标频道和可选转发评论。
     * 输出：目标频道中新创建的转发消息投影。
     * 约束：源消息必须存在，目标频道必须允许当前账号发送消息。
     *
     * @param command 频道消息转发业务命令
     * @return 创建后的转发消息投影
     */
    ChannelMessageResult forwardChannelMessage(ForwardChannelMessageCommand command);

}
