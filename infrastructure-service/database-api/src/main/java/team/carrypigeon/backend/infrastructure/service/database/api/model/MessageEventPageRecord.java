package team.carrypigeon.backend.infrastructure.service.database.api.model;

import java.util.List;

/** 同一读取事务中的流边界与事件；expiredThrough 及之前的变更已清理，不能继续恢复。 */
public record MessageEventPageRecord(long latestSequence, long expiredThrough, List<MessageEventRecord> events) { }
