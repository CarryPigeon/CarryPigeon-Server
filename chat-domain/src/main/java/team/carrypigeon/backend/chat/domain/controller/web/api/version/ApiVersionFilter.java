package team.carrypigeon.backend.chat.domain.controller.web.api.version;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import team.carrypigeon.backend.chat.domain.controller.web.api.error.ApiErrorBody;
import team.carrypigeon.backend.chat.domain.controller.web.api.error.ApiErrorResponse;

import java.io.IOException;
import java.util.UUID;

/**
 * Enforces API version negotiation via {@code Accept: application/vnd.carrypigeon+json; version=1}.
 * <p>
 * See {@code doc/api/10-HTTP+WebSocket协议.md}.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
public class ApiVersionFilter extends OncePerRequestFilter {

    private static final String MEDIA = "application/vnd.carrypigeon+json";
    private static final String EXPECTED_VERSION = "1";

    private final ObjectMapper objectMapper;

    public ApiVersionFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null || !path.startsWith("/api")) {
            return true;
        }
        // WS version is checked in WS auth message (api_version).
        return path.startsWith("/api/ws");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String accept = request.getHeader(HttpHeaders.ACCEPT);
        if (accept != null && accept.contains(MEDIA)) {
            String version = parseVersion(accept);
            if (version != null && !EXPECTED_VERSION.equals(version)) {
                writeUnsupported(request, response);
                return;
            }
        }
        filterChain.doFilter(request, response);
        // Recommend response content type for JSON responses.
        if (!response.isCommitted() && response.getContentType() != null
                && response.getContentType().startsWith(MediaType.APPLICATION_JSON_VALUE)) {
            response.setHeader(HttpHeaders.CONTENT_TYPE, MEDIA + "; version=" + EXPECTED_VERSION);
        }
    }

    private String parseVersion(String accept) {
        // naive but robust enough: find "version=" after the media type occurrence
        int mediaPos = accept.indexOf(MEDIA);
        if (mediaPos < 0) {
            return null;
        }
        int vPos = accept.indexOf("version=", mediaPos);
        if (vPos < 0) {
            return null;
        }
        int start = vPos + "version=".length();
        int end = start;
        while (end < accept.length()) {
            char c = accept.charAt(end);
            if (c == ';' || c == ',' || Character.isWhitespace(c)) {
                break;
            }
            end++;
        }
        String v = accept.substring(start, end).trim();
        return v.isEmpty() ? null : v;
    }

    private void writeUnsupported(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String requestId = (String) request.getAttribute("cp_request_id");
        if (requestId == null || requestId.isBlank()) {
            requestId = "req_" + UUID.randomUUID();
            request.setAttribute("cp_request_id", requestId);
        }
        response.setStatus(406);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiErrorBody body = new ApiErrorBody(406, "api_version_unsupported", "api version unsupported", requestId, null);
        objectMapper.writeValue(response.getOutputStream(), new ApiErrorResponse(body));
    }
}

