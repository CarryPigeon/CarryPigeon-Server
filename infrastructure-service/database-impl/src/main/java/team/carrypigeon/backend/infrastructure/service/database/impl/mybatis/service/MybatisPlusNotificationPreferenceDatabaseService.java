package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service;

import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataAccessException;
import team.carrypigeon.backend.infrastructure.service.database.api.exception.DatabaseServiceException;
import team.carrypigeon.backend.infrastructure.service.database.api.model.NotificationChannelPreferenceRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.model.NotificationServerPreferenceRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.NotificationPreferenceDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.NotificationChannelPreferenceEntity;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.NotificationServerPreferenceEntity;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.NotificationPreferenceMapper;

/**
 * MyBatis-Plus 通知偏好数据库服务。
 * 职责：在 database-impl 中承接服务级与频道级通知偏好的持久化。
 * 边界：只负责记录映射和异常收口，不解释通知模式含义。
 */
public class MybatisPlusNotificationPreferenceDatabaseService implements NotificationPreferenceDatabaseService {

    private final NotificationPreferenceMapper notificationPreferenceMapper;

    public MybatisPlusNotificationPreferenceDatabaseService(NotificationPreferenceMapper notificationPreferenceMapper) {
        this.notificationPreferenceMapper = notificationPreferenceMapper;
    }

    /**
     * 查询账户的服务级通知偏好记录。
     */
    @Override
    public Optional<NotificationServerPreferenceRecord> findServerPreferenceByAccountId(long accountId) {
        return execute(() -> Optional.ofNullable(notificationPreferenceMapper.findServerPreferenceByAccountId(accountId)).map(this::toRecord), "failed to query server notification preference");
    }

    /**
     * 查询账户的频道级通知偏好记录列表。
     */
    @Override
    public List<NotificationChannelPreferenceRecord> listChannelPreferencesByAccountId(long accountId) {
        return execute(() -> notificationPreferenceMapper.listChannelPreferencesByAccountId(accountId).stream().map(this::toRecord).toList(), "failed to query channel notification preferences");
    }

    /**
     * 新增或覆盖服务级通知偏好记录。
     */
    @Override
    public void upsertServerPreference(NotificationServerPreferenceRecord record) {
        executeVoid(() -> notificationPreferenceMapper.upsertServerPreference(toEntity(record)), "failed to upsert server notification preference");
    }

    /**
     * 新增或覆盖频道级通知偏好记录。
     */
    @Override
    public void upsertChannelPreference(NotificationChannelPreferenceRecord record) {
        executeVoid(() -> notificationPreferenceMapper.upsertChannelPreference(toEntity(record)), "failed to upsert channel notification preference");
    }

    private NotificationServerPreferenceRecord toRecord(NotificationServerPreferenceEntity entity) {
        return new NotificationServerPreferenceRecord(entity.getAccountId(), entity.getMode(), entity.getMutedUntil(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    private NotificationChannelPreferenceRecord toRecord(NotificationChannelPreferenceEntity entity) {
        return new NotificationChannelPreferenceRecord(entity.getAccountId(), entity.getChannelId(), entity.getMode(), entity.getMutedUntil(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    private NotificationServerPreferenceEntity toEntity(NotificationServerPreferenceRecord record) {
        NotificationServerPreferenceEntity entity = new NotificationServerPreferenceEntity();
        entity.setAccountId(record.accountId());
        entity.setMode(record.mode());
        entity.setMutedUntil(record.mutedUntil());
        entity.setCreatedAt(record.createdAt());
        entity.setUpdatedAt(record.updatedAt());
        return entity;
    }

    private NotificationChannelPreferenceEntity toEntity(NotificationChannelPreferenceRecord record) {
        NotificationChannelPreferenceEntity entity = new NotificationChannelPreferenceEntity();
        entity.setAccountId(record.accountId());
        entity.setChannelId(record.channelId());
        entity.setMode(record.mode());
        entity.setMutedUntil(record.mutedUntil());
        entity.setCreatedAt(record.createdAt());
        entity.setUpdatedAt(record.updatedAt());
        return entity;
    }

    private <T> T execute(DatabaseOperation<T> operation, String errorMessage) {
        try {
            return operation.run();
        } catch (DataAccessException exception) {
            throw new DatabaseServiceException(errorMessage, exception);
        } catch (RuntimeException exception) {
            throw new DatabaseServiceException(errorMessage, exception);
        }
    }

    private void executeVoid(VoidDatabaseOperation operation, String errorMessage) {
        execute(() -> {
            operation.run();
            return null;
        }, errorMessage);
    }

    @FunctionalInterface
    private interface DatabaseOperation<T> { T run(); }

    @FunctionalInterface
    private interface VoidDatabaseOperation { void run(); }
}
