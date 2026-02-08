package team.carrypigeon.backend.chat.domain.controller.web.api.ws;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket endpoint for API v1 event stream.
 * <p>
 * URL: {@code /api/ws}
 */
@Configuration
@EnableWebSocket
public class ApiWebSocketConfig implements WebSocketConfigurer {

    private final ApiWebSocketHandler handler;

    public ApiWebSocketConfig(ApiWebSocketHandler handler) {
        this.handler = handler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/api/ws")
                .setAllowedOriginPatterns("*");
    }
}

