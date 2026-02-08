package team.carrypigeon.backend.chat.domain.controller.web.api.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import team.carrypigeon.backend.chat.domain.controller.web.api.error.ApiErrorBody;
import team.carrypigeon.backend.chat.domain.controller.web.api.error.ApiErrorResponse;

import java.io.IOException;
import java.util.UUID;

/**
 * HTTP authentication filter for `/api/**`.
 * <p>
 * Behavior:
 * <ul>
 *   <li>For protected endpoints, requires {@code Authorization: Bearer <access_token>}.</li>
 *   <li>Verifies access token via {@link AccessTokenService}.</li>
 *   <li>On success, stores {@code uid} into request attribute {@link ApiAuth#REQ_ATTR_UID}.</li>
 *   <li>On failure, returns {@code 401} with a unified {@link ApiErrorResponse} body.</li>
 * </ul>
 * Public endpoint allowlist is maintained in {@link #shouldNotFilter(HttpServletRequest)}.
 */
@Component
public class ApiAccessTokenFilter extends OncePerRequestFilter {

    private final AccessTokenService accessTokenService;
    private final ObjectMapper objectMapper;

    public ApiAccessTokenFilter(AccessTokenService accessTokenService, ObjectMapper objectMapper) {
        this.accessTokenService = accessTokenService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null) {
            return true;
        }
        if (!path.startsWith("/api")) {
            return true;
        }
        // Public endpoints:
        return path.equals("/api/server")
                || path.equals("/api/gates/required/check")
                || path.equals("/api/plugins/catalog")
                || path.equals("/api/domains/catalog")
                || path.startsWith("/api/plugins/download/")
                || path.startsWith("/api/contracts/")
                || path.startsWith("/api/auth/")
                || path.startsWith("/api/ws")
                || path.startsWith("/api/files/download/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
        String token = extractBearer(auth);
        Long uid = accessTokenService.verify(token);
        if (uid == null) {
            writeUnauthorized(request, response);
            return;
        }
        request.setAttribute(ApiAuth.REQ_ATTR_UID, uid);
        filterChain.doFilter(request, response);
    }

    private String extractBearer(String auth) {
        if (auth == null) {
            return null;
        }
        String prefix = "Bearer ";
        if (!auth.startsWith(prefix)) {
            return null;
        }
        return auth.substring(prefix.length()).trim();
    }

    private void writeUnauthorized(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String requestId = (String) request.getAttribute("cp_request_id");
        if (requestId == null || requestId.isBlank()) {
            requestId = "req_" + UUID.randomUUID();
            request.setAttribute("cp_request_id", requestId);
        }
        response.setStatus(401);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiErrorBody body = new ApiErrorBody(401, "unauthorized", "missing or invalid access token", requestId, null);
        objectMapper.writeValue(response.getOutputStream(), new ApiErrorResponse(body));
    }
}
