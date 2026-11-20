package team.carrypigeon.backend.chat.domain.config.http.openapi;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.security.SecurityRequirement;

/**
 * OpenAPI HTTP 鉴权声明策略。
 * 职责：按当前匿名入口清单为其余 `/api/**` 操作补充 Bearer 声明。
 * 边界：只生成文档，不参与实际请求鉴权。
 */
final class OpenApiSecurityPolicy {

    private OpenApiSecurityPolicy() {
    }

    static void ensureSecurity(Operation operation, String path) {
        if (requiresAuthentication(path)
                && (operation.getSecurity() == null || operation.getSecurity().isEmpty())) {
            operation.addSecurityItem(new SecurityRequirement().addList(OpenApiConfiguration.AUTH_SCHEME_NAME));
        }
    }

    static boolean requiresAuthentication(String path) {
        if (path == null || !path.startsWith("/api/")) {
            return false;
        }
        return switch (path) {
            case "/api/server", "/api/files/download/server/head", "/api/auth/register", "/api/auth/login/pwd",
                "/api/auth/login/email", "/api/auth/email_codes", "/api/auth/refresh", "/api/auth/revoke" -> false;
            default -> true;
        };
    }
}
