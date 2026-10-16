package team.carrypigeon.backend.chat.domain.features.channel.support.persistence;

import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelApplication;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelApplicationStatus;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelApplicationRepository;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelApplicationRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.ChannelApplicationDatabaseService;

/**
 * 基于 database-api 的频道邀请仓储适配器。
 * 职责：在 channel feature 内完成邀请领域模型与数据库契约模型转换。
 * 边界：不包含 SQL 与数据库驱动细节。
 */
@Component
@ConditionalOnProperty(prefix = "cp.infrastructure.service.database", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DatabaseBackedChannelApplicationRepository implements ChannelApplicationRepository {

    private final ChannelApplicationDatabaseService channelApplicationDatabaseService;

    public DatabaseBackedChannelApplicationRepository(ChannelApplicationDatabaseService channelApplicationDatabaseService) {
        this.channelApplicationDatabaseService = channelApplicationDatabaseService;
    }

    @Override
    public Optional<ChannelApplication> findById(long applicationId) {
        return channelApplicationDatabaseService.findById(applicationId).map(this::toDomain);
    }

    /**
     * 列出频道下全部邀请 / 申请记录。
     */
    @Override
    public java.util.List<ChannelApplication> list(long channelId) {
        return channelApplicationDatabaseService.findByChannelId(channelId).stream()
            .map(this::toDomain)
            .toList();
    }

    /**
     * 持久化新的邀请或申请记录。
     */
    @Override
    public void save(ChannelApplication application) {
        channelApplicationDatabaseService.insert(toRecord(application));
    }

    /**
     * 更新既有邀请或申请记录。
     */
    @Override
    public void update(ChannelApplication application) {
        channelApplicationDatabaseService.update(toRecord(application));
    }

    @Override
    public void delete(long applicationId) {
        channelApplicationDatabaseService.delete(applicationId);
    }

    @Override
    public Optional<ChannelApplication> findByRelation(long channelId, long applicantId) {
        return channelApplicationDatabaseService.findByChannelAndApplicant(channelId, applicantId).map(this::toDomain);
    }

    private ChannelApplication toDomain(ChannelApplicationRecord record) {
        return new ChannelApplication(
            record.applicationId(),
            record.applicantId(),
            record.channelId(),
            record.reason(),
            ChannelApplicationStatus.valueOf(record.status()),
            record.createdAt(),
            record.respondedAt()
        );
    }

    private ChannelApplicationRecord toRecord(ChannelApplication channelInvite) {
        return new ChannelApplicationRecord(
            channelInvite.applicationId(),
            channelInvite.applicantId(),
            channelInvite.channelId(),
            channelInvite.reason(),
            channelInvite.status().name(),
            channelInvite.createdAt(),
            channelInvite.respondedAt()
        );
    }
}
