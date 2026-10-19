package team.carrypigeon.backend.chat.domain.features.server.controller.ws;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.server.domain.api.realtime.RealtimeClientMessage;
import team.carrypigeon.backend.chat.domain.features.server.domain.api.realtime.RealtimeCommandContext;
import team.carrypigeon.backend.chat.domain.features.server.domain.api.realtime.RealtimeCommandHandler;

/**
 * 验证上行命令注册表的查找与装配期唯一性约束。
 */
class RealtimeCommandRegistryTests {

    /**
     * 验证已注册的命令类型能查回原处理器实例。
     */
    @Test
    void find_registeredCommandType_returnsSameHandler() {
        RealtimeCommandHandler ping = handler("ping");
        RealtimeCommandRegistry registry = new RealtimeCommandRegistry(List.of(handler("auth"), ping));

        assertSame(ping, registry.find("ping").orElseThrow());
    }

    /**
     * 验证未注册的命令类型返回空，由分派层转换为 unsupported_command。
     */
    @Test
    void find_unknownCommandType_returnsEmpty() {
        RealtimeCommandRegistry registry = new RealtimeCommandRegistry(List.of(handler("ping")));

        assertTrue(registry.find("unknown").isEmpty());
    }

    /**
     * 验证重复命令类型在装配期直接失败，避免后者静默覆盖前者。
     */
    @Test
    void constructor_duplicateCommandType_failsFast() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> new RealtimeCommandRegistry(List.of(handler("ping"), handler("ping")))
        );

        assertTrue(exception.getMessage().contains("ping"));
    }

    /**
     * 验证空白命令类型在装配期被拒绝。
     */
    @Test
    void constructor_blankCommandType_failsFast() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new RealtimeCommandRegistry(List.of(handler(" ")))
        );
    }

    private RealtimeCommandHandler handler(String commandType) {
        return new RealtimeCommandHandler() {

            @Override
            public String commandType() {
                return commandType;
            }

            @Override
            public boolean requiresAuthentication() {
                return true;
            }

            @Override
            public void handle(RealtimeCommandContext context, RealtimeClientMessage command) {
                // 本测试只验证注册表索引，不执行命令逻辑
            }
        };
    }
}
