package team.carrypigeon.backend.chat.domain.features.message.domain.api;

import team.carrypigeon.backend.chat.domain.features.message.domain.command.DeleteChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.command.EditChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.command.RecallChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.projection.ChannelMessageResult;

/**
 * 频道消息生命周期领域 API。
 * 职责：暴露消息编辑、撤回和删除能力。
 * 边界：不暴露消息发布、查询、附件上传和置顶能力。
 * 输入：消息编辑、撤回和删除命令对象。
 * 输出：编辑或撤回后的消息投影；删除以副作用为结果。
 * 失败语义：消息不存在、频道权限不足、编辑窗口过期、版本冲突等问题由领域问题异常表达。
 * 调用方：controller 或其它 feature 通过本接口改变消息生命周期，不直接更新消息仓储。
 */
public interface ChannelMessageLifecycleApi {

    /**
     * 编辑频道消息内容。
     * 输入：命令携带操作者、消息 ID、领域版本、文本内容、mention 和期望编辑版本。
     * 输出：编辑后的频道消息投影。
     * 约束：仅允许符合领域规则的消息类型和发送者在编辑窗口内修改。
     *
     * @param command 频道消息编辑业务命令
     * @return 编辑后的频道消息投影
     */
    ChannelMessageResult editChannelMessage(EditChannelMessageCommand command);

    /**
     * 撤回频道消息。
     * 输入：命令携带操作者、频道 ID 和消息 ID。
     * 输出：撤回后的频道消息投影，内容应按撤回语义脱敏。
     * 副作用：更新消息状态并发布消息更新事件。
     *
     * @param command 频道消息撤回业务命令
     * @return 撤回后的频道消息投影
     */
    ChannelMessageResult recallChannelMessage(RecallChannelMessageCommand command);

    /**
     * 删除频道消息。
     * 输入：命令携带操作者和消息 ID。
     * 副作用：删除目标消息，并按消息更新语义通知相关接收者。
     * 约束：删除权限复用消息治理和撤回权限规则。
     *
     * @param command 频道消息删除业务命令
     */
    void deleteChannelMessage(DeleteChannelMessageCommand command);
}
