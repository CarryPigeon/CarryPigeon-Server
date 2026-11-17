package team.carrypigeon.backend.chat.domain.features.message.domain.api.message;

import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.result.ChannelMessageResult;

import java.util.List;
import java.util.Map;
import java.util.UUID;

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
     * @param requestId 客户端生成的请求 UUID，重试必须保持不变；同一发送者和频道内不得复用于不同载荷
     * @param domain 消息 domain
     * @param data 消息数据，数值 ID 使用 Long；字符串 ID 由请求适配层解析
     * @param mentions 提及的账号列表
     * @return 创建后的频道消息投影
     */
    ChannelMessageResult sendChannelMessage(long accountId, long channelId, UUID requestId, String domain, Map<String, Object> data, List<Long> mentions);

    /**
     * 撤回频道消息并清空可见内容与提醒元数据。
     *
     * @param accountId 操作账号
     * @param channelId 目标频道
     * @param messageId 消息ID
     */
    void recallChannelMessage(long accountId, long channelId, long messageId);
}
