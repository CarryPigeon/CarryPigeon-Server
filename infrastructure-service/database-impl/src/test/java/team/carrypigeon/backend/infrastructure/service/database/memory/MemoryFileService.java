package team.carrypigeon.backend.infrastructure.service.database.memory;

import static team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent.TEST;

import java.time.Clock;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent;
import team.carrypigeon.backend.infrastructure.service.database.api.model.FileRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.FileDatabaseService;

/** File 的内存持久化实现，数据归当前测试容器所有。 */
@EnvironmentComponent(profiles = TEST)
public class MemoryFileService extends LongIdTable<FileRecord> implements FileDatabaseService {
    private final Clock clock;

    public MemoryFileService(MemoryDatabase database, Clock clock) {
        super(database, FileRecord::fileId);
        this.clock = clock;
    }

    @Override
    boolean conflicts(FileRecord a, FileRecord b) {
        return a.shareKey().equals(b.shareKey());
    }

    @Override
    public boolean delete(long id) {
        return deleteById(id);
    }

    @Override
    public boolean deleteByIds(Collection<Long> ids) {
        return database.locked(() -> {
            int before = rows.size();
            rows.keySet().removeAll(ids);
            return before - rows.size() == ids.size();
        });
    }

    @Override
    public List<FileRecord> findByIds(Collection<Long> ids) {
        return super.findByIds(List.copyOf(ids));
    }

    @Override
    public Optional<FileRecord> findByShareKey(String shareKey) {
        return select(r -> shareKey.equals(r.shareKey())).stream().findFirst();
    }

    @Override
    public Optional<FileRecord> findBySha256(String sha256) {
        return select(r -> r.sha256().equals(sha256) && "COMPLETED".equals(r.status())
                && r.expiresAt().isAfter(clock.instant())).stream()
                .max(Comparator.comparing(FileRecord::expiresAt).thenComparingLong(FileRecord::fileId));
    }

    @Override
    public List<FileRecord> findExpired() {
        return select(r -> !r.expiresAt().isAfter(clock.instant()));
    }
}
