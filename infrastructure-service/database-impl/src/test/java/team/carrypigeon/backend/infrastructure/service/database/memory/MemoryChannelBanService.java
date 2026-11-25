package team.carrypigeon.backend.infrastructure.service.database.memory;

import static team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent.TEST;

import java.util.Optional;
import team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelBanRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.model.DatabasePageQuery;
import team.carrypigeon.backend.infrastructure.service.database.api.model.DatabasePageResult;
import team.carrypigeon.backend.infrastructure.service.database.api.service.ChannelBanDatabaseService;

/** ChannelBan 的内存持久化实现，数据归当前测试容器所有。 */
@EnvironmentComponent(profiles = TEST)
public class MemoryChannelBanService extends LongIdTable<ChannelBanRecord> implements ChannelBanDatabaseService {

    public MemoryChannelBanService(MemoryDatabase database) {
        super(database, ChannelBanRecord::banId);
    }

    @Override
    boolean conflicts(ChannelBanRecord a, ChannelBanRecord b) {
        return a.channelId() == b.channelId() && a.bannedAccountId() == b.bannedAccountId();
    }

    @Override
    public Optional<ChannelBanRecord> findByChannelIdAndBannedAccountId(long channelId, long bannedAccountId) {
        return select(r -> r.channelId() == channelId && r.bannedAccountId() == bannedAccountId).stream().findFirst();
    }

    @Override
    public Optional<ChannelBanRecord> findByChannelIdAndBannedAccountIdDirect(long channelId, long bannedAccountId) {
        return findByChannelIdAndBannedAccountId(channelId, bannedAccountId);
    }

    @Override
    public DatabasePageResult<ChannelBanRecord> pageByChannelId(long channelId, DatabasePageQuery page) {
        return pageOf(select(r -> r.channelId() == channelId).reversed(), page);
    }

    @Override
    public void deleteByChannelId(long channelId) {
        deleteWhere(r -> r.channelId() == channelId);
    }
}
