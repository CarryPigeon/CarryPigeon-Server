package team.carrypigeon.backend.chat.domain.config.ws.uplink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;
import team.carrypigeon.backend.infrastructure.basic.realtime.uplink.RealtimeCommandHandler;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;

/**
 * 验证内建命令处理器的自描述定义与容器装配约束。
 * 说明：内建处理器必须各自声明唯一命令类型并可直接被组件扫描装配，不允许靠构造参数注入命令类型。
 */
class BuiltinCommandHandlerTests {

    private final TimeProvider timeProvider = new FixedTimeProvider();

    /**
     * 验证三条内建命令各自声明唯一的命令类型，且应答名与线上协议一致。
     */
    @Test
    void builtinHandlers_declareOwnCommandType() {
        assertEquals("auth", AuthCommandHandler.COMMAND_TYPE);
        assertEquals("auth.ok", AuthCommandHandler.ACK_TYPE);
        assertEquals("reauth", ReauthCommandHandler.COMMAND_TYPE);
        assertEquals("reauth.ok", ReauthCommandHandler.ACK_TYPE);
        assertEquals("ping", PingCommandHandler.COMMAND_TYPE);
        assertEquals("pong", PingCommandHandler.ACK_TYPE);

        List<RealtimeCommandHandler> handlers = List.of(
                new AuthCommandHandler(),
                new ReauthCommandHandler(),
                new PingCommandHandler(timeProvider)
        );
        assertEquals(List.of("auth", "reauth", "ping"), handlers.stream().map(RealtimeCommandHandler::commandType).toList());
    }

    /**
     * 验证 auth 不要求已认证，reauth 与 ping 要求已认证。
     */
    @Test
    void builtinHandlers_declareAuthenticationRequirement() {
        assertFalse(new AuthCommandHandler().requiresAuthentication());
        assertTrue(new ReauthCommandHandler().requiresAuthentication());
        assertTrue(new PingCommandHandler(timeProvider).requiresAuthentication());
    }

    /**
     * 验证三条内建命令都由 `@Component` 装配，而不是由 `@Configuration` 里的 `@Bean` 装配。
     */
    @Test
    void builtinHandlers_areComponents() {
        assertNotNull(AuthCommandHandler.class.getAnnotation(Component.class));
        assertNotNull(ReauthCommandHandler.class.getAnnotation(Component.class));
        assertNotNull(PingCommandHandler.class.getAnnotation(Component.class));
    }

    /**
     * 固定时间提供器。
     * 说明：本测试不校验 ping 应答内容，只需一个可用的时间来源。
     */
    private static final class FixedTimeProvider implements TimeProvider {

        @Override
        public Instant nowInstant() {
            return Instant.EPOCH;
        }

        @Override
        public long nowMillis() {
            return 0L;
        }

        @Override
        public LocalDateTime nowLocalDateTime() {
            return LocalDateTime.ofInstant(Instant.EPOCH, ZoneId.of("UTC"));
        }

        @Override
        public ZoneId zoneId() {
            return ZoneId.of("UTC");
        }
    }
}
