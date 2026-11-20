package team.carrypigeon.backend.chat.domain.config.http.security;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

@Slf4j
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    private final SecurityErrorResponses errors;

    public JsonAccessDeniedHandler(SecurityErrorResponses errors) {
        this.errors = errors;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        log.warn("HTTP access denied, method={}",
                request.getMethod());
        errors.write(request, response, accessDeniedException);
    }
}
