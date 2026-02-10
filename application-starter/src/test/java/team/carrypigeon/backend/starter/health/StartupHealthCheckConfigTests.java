package team.carrypigeon.backend.starter.health;

import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 启动健康检查配置测试。
 */
class StartupHealthCheckConfigTests {

    /**
     * 验证 MySQL 与 Redis 正常时检查通过。
     *
     * @throws Exception 测试执行异常。
     */
    @Test
    void startupHealthChecker_mysqlAndRedisOk_shouldPass() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection conn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        when(dataSource.getConnection()).thenReturn(conn);
        when(conn.prepareStatement(anyString())).thenReturn(ps);
        when(ps.execute()).thenReturn(true);

        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        StartupHealthCheckConfig config = new StartupHealthCheckConfig();
        ApplicationRunner runner = config.startupHealthChecker(dataSource, redisTemplate);
        assertDoesNotThrow(() -> runner.run(mock(ApplicationArguments.class)));

        verify(ps, times(1)).execute();
        verify(valueOps, times(1)).set(anyString(), eq("ok"), eq(Duration.ofSeconds(5)));
        verify(redisTemplate, times(1)).delete(anyString());
        verify(conn, times(1)).close();
        verify(ps, times(1)).close();
    }

    /**
     * 验证 MySQL 不可用时启动检查抛错。
     *
     * @throws Exception 测试执行异常。
     */
    @Test
    void startupHealthChecker_mysqlFail_shouldThrowIllegalStateException() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenThrow(new RuntimeException("db down"));

        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);

        StartupHealthCheckConfig config = new StartupHealthCheckConfig();
        ApplicationRunner runner = config.startupHealthChecker(dataSource, redisTemplate);
        assertThrows(IllegalStateException.class, () -> runner.run(mock(ApplicationArguments.class)));

        verify(redisTemplate, never()).opsForValue();
    }

    /**
     * 验证 Redis 不可用时启动检查抛错。
     *
     * @throws Exception 测试执行异常。
     */
    @Test
    void startupHealthChecker_redisFail_shouldThrowIllegalStateException() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection conn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        when(dataSource.getConnection()).thenReturn(conn);
        when(conn.prepareStatement(anyString())).thenReturn(ps);
        when(ps.execute()).thenReturn(true);

        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        doThrow(new RuntimeException("redis down")).when(valueOps).set(anyString(), anyString(), any(Duration.class));

        StartupHealthCheckConfig config = new StartupHealthCheckConfig();
        ApplicationRunner runner = config.startupHealthChecker(dataSource, redisTemplate);
        assertThrows(IllegalStateException.class, () -> runner.run(mock(ApplicationArguments.class)));
    }
}
