package team.carrypigeon.backend.chat.domain.http.security;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/***
 * 无认证响应
 * 职责：当用户未认证时，返回JSON格式的错误信息
 * 边界：只处理未认证的错误信息
 * */
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final SecurityErrorResponses errors;

    public JsonAuthenticationEntryPoint(SecurityErrorResponses errors) {
        this.errors = errors;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        errors.write(request, response, authException);
    }
}

