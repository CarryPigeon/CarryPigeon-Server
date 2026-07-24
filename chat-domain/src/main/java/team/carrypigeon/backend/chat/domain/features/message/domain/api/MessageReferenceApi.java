package team.carrypigeon.backend.chat.domain.features.message.domain.api;

import team.carrypigeon.backend.chat.domain.features.message.domain.projection.MessageReferenceResult;

/**
 * 消息引用查询 API。
 * 职责：向 channel 等 feature 暴露消息存在性与频道归属的最小查询能力。
 * 边界：不暴露消息聚合、仓储或消息正文。
 * 输入：消息 ID 或频道 ID。
 * 输出：消息引用投影或频道消息存在性。
 * 失败语义：必须存在的消息缺失时由领域问题异常表达。
 * 调用方：channel 等 feature 只能依赖本接口，不直接访问 message 仓储。
 */
public interface MessageReferenceApi {

    /**
     * 读取必须存在的消息引用。
     *
     * @param messageId 消息 ID
     * @return 消息引用投影
     */
    MessageReferenceResult requireMessage(long messageId);

    /**
     * 判断频道是否已存在消息。
     *
     * @param channelId 频道 ID
     * @return 存在任意消息时返回 true
     */
    boolean hasChannelMessages(long channelId);
}
