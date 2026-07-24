package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper;

import java.lang.reflect.Method;
import java.util.Arrays;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 消息幂等 Mapper SQL 契约测试。
 * 职责：保护并发幂等依赖的唯一键占位、行锁和一次性结果绑定语义。
 */
@Tag("contract")
class MessageIdempotencyMapperTests {

    /**
     * 验证预留 SQL 使用原子 upsert，避免先查后插竞争窗口。
     */
    @Test
    @DisplayName("reserve sql uses atomic duplicate key handling")
    void reserveSql_contract_usesAtomicDuplicateKeyHandling() throws Exception {
        Method method = Arrays.stream(MessageIdempotencyMapper.class.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals("reserve"))
                .findFirst()
                .orElseThrow();
        String sql = String.join(" ", method.getAnnotation(Insert.class).value()).toUpperCase();

        assertTrue(sql.contains("ON DUPLICATE KEY UPDATE"));
    }

    /**
     * 验证预留读取持有排他行锁直到调用方事务完成。
     */
    @Test
    @DisplayName("find for update sql locks reservation")
    void findForUpdateSql_contract_locksReservation() throws Exception {
        Method method = Arrays.stream(MessageIdempotencyMapper.class.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals("findForUpdate"))
                .findFirst()
                .orElseThrow();
        String sql = String.join(" ", method.getAnnotation(Select.class).value()).toUpperCase();

        assertTrue(sql.contains("FOR UPDATE"));
    }

    /**
     * 验证完成 SQL 只允许尚未绑定结果且指纹一致的记录更新一次。
     */
    @Test
    @DisplayName("complete sql binds result once with fingerprint")
    void completeSql_contract_bindsResultOnceWithFingerprint() throws Exception {
        Method method = Arrays.stream(MessageIdempotencyMapper.class.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals("complete"))
                .findFirst()
                .orElseThrow();
        String sql = String.join(" ", method.getAnnotation(Update.class).value()).toUpperCase();

        assertTrue(sql.contains("REQUEST_FINGERPRINT = #{REQUESTFINGERPRINT}"));
        assertTrue(sql.contains("MESSAGE_ID IS NULL"));
    }
}
