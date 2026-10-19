package team.carrypigeon.backend.chat.domain.config.ws.uplink;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import team.carrypigeon.backend.infrastructure.basic.realtime.uplink.RealtimeCommandHandler;

/**
 * 实时上行命令注册表。
 * 职责：按命令类型索引容器中的所有处理器，并在装配期拒绝重复类型。
 * 边界：只做查找与唯一性校验，不执行命令、不管理连接、不持有任何内建命令清单。
 */
@Component
public final class RealtimeCommandRegistry {

    private final Map<String, RealtimeCommandHandler> handlersByType;

    public RealtimeCommandRegistry(List<RealtimeCommandHandler> handlers) {
        Map<String, RealtimeCommandHandler> byType = new LinkedHashMap<>();
        for (RealtimeCommandHandler handler : handlers) {
            String commandType = handler.commandType();
            if (commandType == null || commandType.isBlank()) {
                throw new IllegalArgumentException("realtime command type must not be blank");
            }
            RealtimeCommandHandler previous = byType.putIfAbsent(commandType, handler);
            if (previous != null) {
                throw new IllegalStateException("duplicate realtime command type: " + commandType);
            }
        }
        this.handlersByType = Map.copyOf(byType);
    }

    /**
     * 按命令类型查找处理器。
     *
     * @param commandType 客户端命令类型
     * @return 命中的处理器，未注册时为空
     */
    public Optional<RealtimeCommandHandler> find(String commandType) {
        return Optional.ofNullable(handlersByType.get(commandType));
    }
}
