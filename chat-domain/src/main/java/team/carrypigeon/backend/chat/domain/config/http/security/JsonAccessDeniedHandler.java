package team.carrypigeon.backend.chat.domain.config.http.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;

import java.io.IOException;

@Slf4j
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    private final JsonProvider jsonProvider;

    public JsonAccessDeniedHandler(JsonProvider jsonProvider) {
        this.jsonProvider = jsonProvider;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        log.warn("HTTP access denied, method={}",
                request.getMethod());
        SecurityErrorResponses.write(
                jsonProvider,
                response,
                HttpStatus.FORBIDDEN.value(),
                "forbidden",
                "access denied",
                null
        );
    }
}
