package team.carrypigeon.backend.infrastructure.service.database.api.model;

/** 文件状态表的账号配额聚合；在账号配额行锁内读取，不依赖缓存或可能漂移的计数器。 */
public record FileStorageUsageRecord(long allocatedBytes, long unfinishedUploads) {}
