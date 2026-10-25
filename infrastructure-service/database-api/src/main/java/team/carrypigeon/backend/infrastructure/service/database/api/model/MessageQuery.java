package team.carrypigeon.backend.infrastructure.service.database.api.model;

/**
 * 消息表通用查询条件。
 * 职责：只承载数据谓词、排序方向和条数，不携带消息业务规则。
 * 边界：不判断消息状态含义、不决定时间线语义；业务选择由 domain Repository 完成。
 *
 * @param channelId 频道 ID
 * @param beforeMessageId 只查询该消息 ID 之前的数据；可为空
 * @param afterMessageId 只查询该消息 ID 之后的数据；可为空
 * @param senderAccountId 发送者账号 ID；可为空
 * @param domain 消息 domain；可为空
 * @param status 消息状态；可为空
 * @param ascending 是否按消息 ID 升序
 * @param limit 最大返回条数
 */
public record MessageQuery(
        long channelId,
        Long beforeMessageId,
        Long afterMessageId,
        Long senderAccountId,
        String domain,
        String status,
        boolean ascending,
        int limit
) {
}
