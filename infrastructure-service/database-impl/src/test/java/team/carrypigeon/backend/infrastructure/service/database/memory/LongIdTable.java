package team.carrypigeon.backend.infrastructure.service.database.memory;

import java.util.Comparator;
import java.util.Optional;
import java.util.function.Function;

/** 为以单个 long 主键访问的数据库服务提供与公共接口一致的查询和删除签名。 */
abstract class LongIdTable<R> extends MemoryTable<Long, R> {
    LongIdTable(MemoryDatabase database, Function<R, Long> keyOf) {
        super(database, keyOf, Comparator.naturalOrder());
    }

    public Optional<R> findById(long id) {
        return byKey(id);
    }

    public boolean deleteById(long id) {
        return deleteKey(id);
    }
}
