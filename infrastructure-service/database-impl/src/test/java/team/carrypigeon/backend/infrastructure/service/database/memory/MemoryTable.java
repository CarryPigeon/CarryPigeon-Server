package team.carrypigeon.backend.infrastructure.service.database.memory;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Predicate;
import org.springframework.dao.DuplicateKeyException;
import team.carrypigeon.backend.infrastructure.service.database.api.model.DatabasePageQuery;
import team.carrypigeon.backend.infrastructure.service.database.api.model.DatabasePageResult;

/** 测试用有序记录表。Record 均为不可变值，因此浅拷贝即可恢复一个事务开始时的表内容。 */
abstract class MemoryTable<K, R> {
    protected final MemoryDatabase database;
    protected final Map<K, R> rows;
    private final Function<R, K> keyOf;

    MemoryTable(MemoryDatabase database, Function<R, K> keyOf, Comparator<K> order) {
        this.database = database;
        this.keyOf = keyOf;
        this.rows = new TreeMap<>(order);
        database.register(() -> {
            var before = new TreeMap<K, R>(order);
            before.putAll(rows);
            return () -> {
                rows.clear();
                rows.putAll(before);
            };
        });
    }

    boolean conflicts(R first, R second) {
        return false;
    }

    private void checkUnique(R record) {
        K key = keyOf.apply(record);
        if (rows.entrySet().stream().anyMatch(e -> !e.getKey().equals(key) && conflicts(record, e.getValue()))) {
            throw new DuplicateKeyException("Duplicate relation in " + getClass().getSimpleName());
        }
    }

    public boolean insert(R record) {
        return database.locked(() -> {
            checkUnique(record);
            if (rows.putIfAbsent(keyOf.apply(record), record) != null) {
                throw new DuplicateKeyException("Duplicate primary key in " + getClass().getSimpleName());
            }
            return true;
        });
    }

    public boolean insertAll(List<R> records) {
        if (records == null || records.isEmpty()) {
            return true;
        }
        return database.runInTransaction(() -> records.stream().allMatch(this::insert));
    }

    public boolean update(R record) {
        return database.locked(() -> {
            checkUnique(record);
            return rows.replace(keyOf.apply(record), record) != null;
        });
    }

    protected Optional<R> byKey(K key) {
        return database.locked(() -> {
            return Optional.ofNullable(rows.get(key));
        });
    }

    public List<R> findByIds(List<K> keys) {
        return database.locked(() -> {
            if (keys == null || keys.isEmpty()) {
                return List.of();
            }
            return keys.stream().distinct().filter(rows::containsKey).map(rows::get).toList();
        });
    }

    protected boolean deleteKey(K key) {
        return database.locked(() -> {
            return rows.remove(key) != null;
        });
    }

    public boolean deleteByIds(List<K> keys) {
        return database.locked(() -> {
            return keys == null || keys.isEmpty() || rows.keySet().removeAll(keys);
        });
    }

    protected List<R> select(Predicate<R> filter) {
        return database.locked(() -> {
            return rows.values().stream().filter(filter).toList();
        });
    }

    protected void deleteWhere(Predicate<R> filter) {
        database.locked(() -> {
            rows.values().removeIf(filter);
            return null;
        });
    }

    public DatabasePageResult<R> page(DatabasePageQuery query) {
        return pageOf(select(row -> true), query);
    }

    protected DatabasePageResult<R> pageOf(Collection<R> records, DatabasePageQuery query) {
        var items = records.stream().skip(query.offset()).limit(query.pageSize()).toList();
        return new DatabasePageResult<>(items, query.page(), query.pageSize(), records.size());
    }
}
