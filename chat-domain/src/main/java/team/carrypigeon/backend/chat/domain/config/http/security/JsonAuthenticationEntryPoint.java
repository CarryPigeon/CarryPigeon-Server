package team.carrypigeon.backend.chat.domain.config.http.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;

import java.io.IOException;

/***
 * 无认证响应
 * 职责：当用户未认证时，返回JSON格式的错误信息
 * 边界：只处理未认证的错误信息
 * */
@Slf4j
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final JsonProvider jsonProvider;

    public JsonAuthenticationEntryPoint(JsonProvider jsonProvider) {
        this.jsonProvider = jsonProvider;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        log.info("HTTP authentication required, method={}",
                request.getMethod());
        SecurityErrorResponses.write(
                jsonProvider,
                response,
                HttpStatus.UNAUTHORIZED.value(),
                "unauthorized",
                "authentication is required",
                null
        );
    }
}

