package team.carrypigeon.backend.infrastructure.service.database.memory;

import static team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent.TEST;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelMemberRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.model.DatabasePageQuery;
import team.carrypigeon.backend.infrastructure.service.database.api.model.DatabasePageResult;
import team.carrypigeon.backend.infrastructure.service.database.api.service.ChannelMemberDatabaseService;

/** ChannelMember 的内存持久化实现，数据归当前测试容器所有。 */
@EnvironmentComponent(profiles = TEST)
public class MemoryChannelMemberService extends LongIdTable<ChannelMemberRecord> implements ChannelMemberDatabaseService {
    private final ObjectProvider<MemoryChannelService> channels;

    public MemoryChannelMemberService(MemoryDatabase database, ObjectProvider<MemoryChannelService> channels) {
        super(database, ChannelMemberRecord::memberId);
        this.channels = channels;
    }

    @Override
    boolean conflicts(ChannelMemberRecord a, ChannelMemberRecord b) {
        return a.channelId() == b.channelId() && a.accountId() == b.accountId();
    }

    @Override
    public List<Long> findChannelIdsByAccountId(long accountId) {
        return database.locked(() -> {
            return select(r -> r.accountId() == accountId && channels.getObject().findById(r.channelId()).isPresent())
                    .stream().map(ChannelMemberRecord::channelId).toList();
        });
    }

    @Override
    public Optional<ChannelMemberRecord> findByChannelIdAndAccountId(long channelId, long accountId) {
        return select(r -> r.channelId() == channelId && r.accountId() == accountId).stream().findFirst();
    }

    @Override
    public Optional<ChannelMemberRecord> findByChannelIdAndAccountIdDirect(long channelId, long accountId) {
        return findByChannelIdAndAccountId(channelId, accountId);
    }

    @Override
    public Optional<ChannelMemberRecord> findByChannelIdAndAccountIdForUpdate(long channelId, long accountId) {
        return findByChannelIdAndAccountId(channelId, accountId);
    }

    @Override
    public List<ChannelMemberRecord> findMembersByChannelId(long channelId) {
        return select(r -> r.channelId() == channelId);
    }

    @Override
    public DatabasePageResult<ChannelMemberRecord> pageByAccountId(long accountId, DatabasePageQuery page) {
        return database.locked(() -> {
            var visible = select(r -> r.accountId() == accountId && channels.getObject().findById(r.channelId()).isPresent());
            return pageOf(visible.stream().sorted(Comparator.comparingLong(ChannelMemberRecord::channelId).reversed()).toList(), page);
        });
    }

    @Override
    public DatabasePageResult<ChannelMemberRecord> pageByChannelId(long channelId, DatabasePageQuery page) {
        return pageOf(select(r -> r.channelId() == channelId).reversed(), page);
    }

    @Override
    public void deleteByChannelId(long channelId) {
        deleteWhere(r -> r.channelId() == channelId);
    }

    @Override
    public List<Long> findMemberAccountIds(long channelId, List<Long> candidates) {
        return findMembersByChannelId(channelId).stream().map(ChannelMemberRecord::accountId)
                .filter(candidates::contains).sorted().toList();
    }
}
