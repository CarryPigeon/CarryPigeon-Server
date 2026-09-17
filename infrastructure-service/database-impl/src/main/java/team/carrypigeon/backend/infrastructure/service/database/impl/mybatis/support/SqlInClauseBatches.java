package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * SQL IN 条件分片器。
 * 职责：对批量查询 ID 去重并拆成固定上限的小集合，避免单条 SQL 参数无限增长。
 * 边界：只处理集合分片，不执行 SQL、不解释业务 ID 语义。
 */
public final class SqlInClauseBatches {

    public static final int MAX_VALUES_PER_BATCH = 500;

    private SqlInClauseBatches() {
    }

    /**
     * 按固定 SQL 参数上限分片，并保持输入首次出现的顺序。
     */
    public static <T> List<List<T>> partition(Collection<T> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        List<T> distinctValues = new ArrayList<>(new LinkedHashSet<>(values));
        List<List<T>> batches = new ArrayList<>((distinctValues.size() + MAX_VALUES_PER_BATCH - 1) / MAX_VALUES_PER_BATCH);
        for (int start = 0; start < distinctValues.size(); start += MAX_VALUES_PER_BATCH) {
            batches.add(List.copyOf(distinctValues.subList(
                    start,
                    Math.min(start + MAX_VALUES_PER_BATCH, distinctValues.size())
            )));
        }
        return List.copyOf(batches);
    }
}
