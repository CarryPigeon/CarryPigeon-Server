package team.carrypigeon.backend.chat.domain.http.security;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    private final SecurityErrorResponses errors;

    public JsonAccessDeniedHandler(SecurityErrorResponses errors) {
        this.errors = errors;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        errors.write(request, response, accessDeniedException);
    }
}
