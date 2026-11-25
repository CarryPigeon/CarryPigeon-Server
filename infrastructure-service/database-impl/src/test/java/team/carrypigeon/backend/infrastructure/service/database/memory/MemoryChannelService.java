package team.carrypigeon.backend.infrastructure.service.database.memory;

import static team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent.TEST;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelDiscoverRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.ChannelDatabaseService;

/** Channel 的内存持久化实现，数据归当前测试容器所有。 */
@EnvironmentComponent(profiles = TEST)
public class MemoryChannelService extends LongIdTable<ChannelRecord> implements ChannelDatabaseService {
    private final MemoryChannelMemberService members;

    public MemoryChannelService(MemoryDatabase database, MemoryChannelMemberService members) {
        super(database, ChannelRecord::id);
        this.members = members;
    }

    @Override
    public Optional<ChannelRecord> findByIdDirect(long channelId) {
        return findById(channelId);
    }

    @Override
    public Optional<ChannelRecord> findByIdForUpdate(long channelId) {
        return findById(channelId);
    }

    @Override
    public List<ChannelRecord> findSystemChannels() {
        return select(ChannelRecord::systemChannel);
    }

    @Override
    public List<ChannelDiscoverRecord> findPublicAfterId(String keyword, Long cursorChannelId, int limit) {
        return database.locked(() -> {
            return select(r -> !r.systemChannel() && (cursorChannelId == null || r.id() > cursorChannelId)
                    && (keyword == null || r.name().toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT))
                    || r.brief().toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT)))).stream()
                    .limit(limit)
                    .map(r -> new ChannelDiscoverRecord(r.id(), r.name(), r.brief(), null,
                            members.findMembersByChannelId(r.id()).size(), true))
                    .toList();
        });
    }
}
