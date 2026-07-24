package team.carrypigeon.backend.chat.domain.features.message.domain.api;

import team.carrypigeon.backend.chat.domain.features.message.domain.command.RecallChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.projection.ChannelMessageResult;

/**
 * 频道消息生命周期领域 API。
 * 职责：只暴露消息撤回能力。
 * 边界：消息不支持编辑或用户可见硬删除；发布、查询、附件和置顶由其它 API 承担。
 * 输入：消息撤回命令。
 * 输出：撤回后的 canonical 消息投影。
 * 失败语义：消息不存在、频道不匹配或撤回权限不足由领域问题异常表达。
 */
public interface ChannelMessageLifecycleApi {

    /**
     * 撤回频道消息并清空可见内容与提醒元数据。
     *
     * @param command 频道消息撤回命令
     * @return 撤回后的 canonical 消息投影
     */
    ChannelMessageResult recallChannelMessage(RecallChannelMessageCommand command);
}
