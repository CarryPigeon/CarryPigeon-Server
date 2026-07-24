package team.carrypigeon.backend.infrastructure.service.database.api.model;

import java.time.Instant;

/**
 * 消息数据库 canonical 记录契约。
 * 职责：表达消息查询、写入与撤回更新共用的最小持久化投影。
 * 边界：data 与 mentions 保持 JSON 文本，不在 database-api 解释 domain 业务结构。
 *
 * @param messageId 消息 ID
 * @param senderId 发送者账户 ID
 * @param channelId 频道 ID
 * @param domain 消息 domain
 * @param domainVersion domain 版本
 * @param data domain 专属数据 JSON object
 * @param sendTime 发送时间
 * @param mentions 提醒用户 ID JSON array
 * @param preview 通用摘要
 * @param status 消息状态
 */
public record MessageRecord(
        long messageId,
        long senderId,
        long channelId,
        String domain,
        String domainVersion,
        String data,
        Instant sendTime,
        String mentions,
        String preview,
        String status
) {
}
