package team.carrypigeon.backend.chat.domain.features.message.domain.api.message;

import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.result.ChannelMessageResult;

import java.util.List;
import java.util.Map;

/**
 * 频道消息发布领域 API。
 * 职责：暴露 canonical 频道消息创建与撤回能力。
 */
public interface ChannelMessageApi {

    /**
     * 按 canonical envelope 发送频道消息。
     *
     * @param accountId 操作账号
     * @param channelId 目标频道
     * @param domain 消息 domain
     * @param data 消息数据
     * @param mentions 提及的账号列表
     * @return 创建后的频道消息投影
     */
    ChannelMessageResult sendChannelMessage(long accountId, long channelId, String domain, Map<String, Object> data, List<Long> mentions);

    /**
     * 撤回频道消息并清空可见内容与提醒元数据。
     *
     * @param accountId 操作账号
     * @param channelId 目标频道
     * @param messageId 消息ID
     */
    void recallChannelMessage(long accountId, long channelId, long messageId);
}
