package team.carrypigeon.backend.infrastructure.service.database.memory;

import static team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent.TEST;

import java.util.Optional;
import team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelApplicationRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.model.DatabasePageQuery;
import team.carrypigeon.backend.infrastructure.service.database.api.model.DatabasePageResult;
import team.carrypigeon.backend.infrastructure.service.database.api.service.ChannelApplicationDatabaseService;

/** ChannelApplication 的内存持久化实现，数据归当前测试容器所有。 */
@EnvironmentComponent(profiles = TEST)
public class MemoryChannelApplicationService extends LongIdTable<ChannelApplicationRecord> implements ChannelApplicationDatabaseService {

    public MemoryChannelApplicationService(MemoryDatabase database) {
        super(database, ChannelApplicationRecord::applicationId);
    }

    @Override
    boolean conflicts(ChannelApplicationRecord a, ChannelApplicationRecord b) {
        return a.channelId() == b.channelId() && a.applicantId() == b.applicantId();
    }

    @Override
    public Optional<ChannelApplicationRecord> findByChannelIdAndApplicantId(long channelId, long applicantId) {
        return select(r -> r.channelId() == channelId && r.applicantId() == applicantId).stream().findFirst();
    }

    @Override
    public DatabasePageResult<ChannelApplicationRecord> pageByChannelId(long channelId, DatabasePageQuery page) {
        return pageOf(select(r -> r.channelId() == channelId).reversed(), page);
    }

    @Override
    public DatabasePageResult<ChannelApplicationRecord> pageByApplicantAccountId(long applicantId, DatabasePageQuery page) {
        return pageOf(select(r -> r.applicantId() == applicantId).reversed(), page);
    }

    @Override
    public boolean decideIfPending(ChannelApplicationRecord record) {
        return database.locked(() -> {
            return findById(record.applicationId())
                    .filter(r -> r.channelId() == record.channelId() && "PENDING".equals(r.status()))
                    .map(r -> update(new ChannelApplicationRecord(r.applicationId(), r.applicantId(), r.channelId(),
                            r.reason(), record.status(), r.createdAt(), record.respondedAt())))
                    .orElse(false);
        });
    }

    @Override
    public void deleteByChannelId(long channelId) {
        deleteWhere(r -> r.channelId() == channelId);
    }
}
