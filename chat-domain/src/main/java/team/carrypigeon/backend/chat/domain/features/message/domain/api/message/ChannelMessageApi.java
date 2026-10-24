package team.carrypigeon.backend.chat.domain.features.message.domain.api.message;

import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.input.RecallChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.input.SendChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.result.ChannelMessageResult;

/**
 * 频道消息发布领域 API。
 * 职责：暴露 canonical 频道消息创建与消息转发能力。
 * 边界：不暴露编辑、撤回、删除、历史查询、搜索、附件上传和置顶能力。
 * 输入：canonical 消息发送与转发命令对象。
 * 输出：创建后的频道消息投影。
 * 失败语义：频道权限、消息类型、领域版本、附件引用和消息源不存在等问题由领域问题异常表达。
 * 调用方：message HTTP controller 通过本接口创建频道消息。
 */
public interface ChannelMessageApi {

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
     * 撤回频道消息并清空可见内容与提醒元数据。
     *
     * @param command 频道消息撤回命令
     * @return 撤回后的 canonical 消息投影
     */
    ChannelMessageResult recallChannelMessage(RecallChannelMessageCommand command);
}
