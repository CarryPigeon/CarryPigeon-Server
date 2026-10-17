package team.carrypigeon.backend.chat.domain.features.channel.support.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelApplication;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelApplicationStatus;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelApplicationRepository;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelApplicationRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.ChannelApplicationDatabaseService;

/**
 * 基于 database-api 的频道申请仓储适配器。
 * 职责：把 {@link ChannelApplicationRepository} 的领域语义调用翻译为 database-api 的最小数据库契约，并完成申请记录的双向转换。
 * 边界：不含 SQL 与数据库驱动细节，不承载申请状态流转、重复申请判定与审批权限规则；事务边界由调用方决定。
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

    @Override
    public List<ChannelApplication> list(long channelId) {
        return channelApplicationDatabaseService.findByChannelId(channelId).stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public void save(ChannelApplication application) {
        channelApplicationDatabaseService.insert(toRecord(application));
    }

    @Override
    public void update(ChannelApplication application) {
        channelApplicationDatabaseService.update(toRecord(application));
    }

    @Override
    public void delete(long applicationId) {
        // 物理删除；重新申请场景会先写入新记录再删除旧记录，以保持同一 (频道, 申请人) 至多一条
        channelApplicationDatabaseService.delete(applicationId);
    }

    @Override
    public Optional<ChannelApplication> findByRelation(long channelId, long applicantId) {
        // 底层为单行查询，调用方依赖"同一 (频道, 申请人) 至多一行"这一前提，重复行会导致查询直接失败
        return channelApplicationDatabaseService.findByChannelAndApplicant(channelId, applicantId).map(this::toDomain);
    }

    /**
     * 把申请记录持久化契约转换为领域模型。
     * 说明：status 由字符串还原为固定枚举，非法取值会抛 IllegalArgumentException；respondedAt 为空表示尚未审批。
     *
     * @param record 频道申请持久化投影
     * @return 频道申请领域模型
     */
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

    /**
     * 把申请记录领域模型转换为持久化契约。
     * 说明：status 以枚举名落库，因此枚举常量改名等同于数据迁移。
     *
     * @param application 频道申请领域模型
     * @return 频道申请持久化投影
     */
    private ChannelApplicationRecord toRecord(ChannelApplication application) {
        return new ChannelApplicationRecord(
            application.applicationId(),
            application.applicantId(),
            application.channelId(),
            application.reason(),
            application.status().name(),
            application.createdAt(),
            application.respondedAt()
        );
    }
}
