package team.carrypigeon.backend.chat.domain.features.message.domain.model;

import java.util.List;

/** 消息变更读取结果；流边界与当前页在同一事务中读取，分页裁剪由协议入口完成。 */
public record MessageChangePage(long latestSequence, long expiredThrough, List<MessageChange> events) { }
