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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class StartupHealthCheckConfigTests {

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

