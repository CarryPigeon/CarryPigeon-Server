package team.carrypigeon.backend.infrastructure.service.database.api.model;

/**
 * 提及表通用查询条件。
 * 职责：只承载数据谓词、排序方向和条数，不解释未读业务含义。
 * 边界：read 为 null 表示不按已读状态过滤；业务层的 unreadOnly 转换由 Repository 完成。
 *
 * @param accountId 被提及账号 ID
 * @param beforeMentionId 只查询该提及 ID 之前的数据；可为空
 * @param read 已读状态过滤值；可为空
 * @param channelId 频道 ID；可为空
 * @param limit 最大返回条数
 */
public record MentionQuery(
        long accountId,
        Long beforeMentionId,
        Boolean read,
        Long channelId,
        int limit
) {
}
