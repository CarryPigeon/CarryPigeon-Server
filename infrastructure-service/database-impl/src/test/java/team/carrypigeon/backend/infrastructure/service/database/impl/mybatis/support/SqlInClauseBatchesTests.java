package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.support;

import java.util.List;
import java.util.stream.LongStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * SQL IN 条件分片器测试。
 * 职责：验证去重、顺序和固定参数上限契约。
 */
@Tag("unit")
class SqlInClauseBatchesTests {

    /** 验证超过上限的集合会分片，重复 ID 不会产生额外参数。 */
    @Test
    @DisplayName("partition oversized collection deduplicates and limits batches")
    void partition_oversizedCollection_deduplicatesAndLimitsBatches() {
        List<Long> values = new java.util.ArrayList<>(LongStream.rangeClosed(1L, 1001L).boxed().toList());
        values.add(1L);

        List<List<Long>> batches = SqlInClauseBatches.partition(values);

        assertEquals(List.of(500, 500, 1), batches.stream().map(List::size).toList());
        assertEquals(1L, batches.getFirst().getFirst());
        assertEquals(1001L, batches.getLast().getLast());
    }
}
