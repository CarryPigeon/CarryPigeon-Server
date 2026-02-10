package team.carrypigeon.backend.chat.domain.controller.web.api.ws;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * API WebSocket 端点配置。
 * <p>
 * 注册 `/api/ws` 路径到 {@link ApiWebSocketHandler}。
 */
@Configuration
@EnableWebSocket
public class ApiWebSocketConfig implements WebSocketConfigurer {

    private final ApiWebSocketHandler handler;

    /**
     * 构造 WebSocket 配置。
     *
     * @param handler WebSocket 协议处理器。
     */
    public ApiWebSocketConfig(ApiWebSocketHandler handler) {
        this.handler = handler;
    }

    /**
     * 注册 WebSocket 路由。
     *
     * @param registry WebSocket 路由注册器。
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/api/ws")
                .setAllowedOriginPatterns("*");
    }
}
