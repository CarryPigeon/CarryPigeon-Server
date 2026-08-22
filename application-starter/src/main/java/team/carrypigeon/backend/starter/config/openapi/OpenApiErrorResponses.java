package team.carrypigeon.backend.starter.config.openapi;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;

/**
 * OpenAPI 通用错误响应装配器。
 * 职责：根据路由能力补充稳定的认证、校验、冲突、外部服务不可用与内部错误示例。
 * 边界：只补充缺失响应，不覆盖控制器已经声明的响应模型。
 */
final class OpenApiErrorResponses {

    private static final String JSON_MEDIA_TYPE = "application/json";

    private OpenApiErrorResponses() {
    }

    static void ensureCommonErrorResponses(Operation operation, String path, String httpMethod) {
        if (operation.getResponses() == null) {
            operation.setResponses(new ApiResponses());
        }

        addErrorResponse(operation.getResponses(), "422", "请求参数或请求体校验失败", "{\n  \"error\": {\n    \"status\": 422,\n    \"reason\": \"validation_failed\",\n    \"message\": \"validation failed\",\n    \"request_id\": \"req_01HXYZ\",\n    \"details\": {\n      \"field_errors\": [\n        {\n          \"field\": \"limit\",\n          \"reason\": \"invalid\",\n          \"message\": \"limit must be between 1 and 50\"\n        }\n      ]\n    }\n  }\n}");
        addErrorResponse(operation.getResponses(), "500", "服务端内部错误", "{\n  \"error\": {\n    \"status\": 500,\n    \"reason\": \"internal_error\",\n    \"message\": \"internal server error\",\n    \"request_id\": \"req_01HXYZ\"\n  }\n}");

        if (supportsUnauthorized(path)) {
            addErrorResponse(operation.getResponses(), "401", "认证缺失、过期或凭据无效", "{\n  \"error\": {\n    \"status\": 401,\n    \"reason\": \"unauthorized\",\n    \"message\": \"authentication is required\",\n    \"request_id\": \"req_01HXYZ\"\n  }\n}");
        }
        if (OpenApiSecurityPolicy.requiresAuthentication(path) || "/api/auth/login".equals(path)) {
            addErrorResponse(operation.getResponses(), "403", "已认证但无权执行该操作", "{\n  \"error\": {\n    \"status\": 403,\n    \"reason\": \"forbidden\",\n    \"message\": \"forbidden\",\n    \"request_id\": \"req_01HXYZ\"\n  }\n}");
        }
        if (supportsNotFound(path)) {
            addErrorResponse(operation.getResponses(), "404", "目标资源不存在", "{\n  \"error\": {\n    \"status\": 404,\n    \"reason\": \"not_found\",\n    \"message\": \"resource does not exist\",\n    \"request_id\": \"req_01HXYZ\"\n  }\n}");
        }
        if ("/api/auth/tokens".equals(path)) {
            addErrorResponse(operation.getResponses(), "412", "required gate 未满足", "{\n  \"error\": {\n    \"status\": 412,\n    \"reason\": \"required_plugin_missing\",\n    \"message\": \"required plugins are missing\",\n    \"request_id\": \"req_01HXYZ\",\n    \"details\": {\n      \"missing_plugins\": [\"mc-bind\"]\n    }\n  }\n}");
        }
        if (supportsConflict(path, httpMethod)) {
            addErrorResponse(operation.getResponses(), "409", "资源状态冲突", "{\n  \"error\": {\n    \"status\": 409,\n    \"reason\": \"conflict\",\n    \"message\": \"resource state conflict\",\n    \"request_id\": \"req_01HXYZ\"\n  }\n}");
        }
        addErrorResponse(operation.getResponses(), "406", "请求的响应媒体类型不受支持", "{\n  \"error\": {\n    \"status\": 406,\n    \"reason\": \"not_acceptable\",\n    \"message\": \"requested response media type is not supported\",\n    \"request_id\": \"req_01HXYZ\"\n  }\n}");
        if (operation.getRequestBody() != null) {
            addErrorResponse(operation.getResponses(), "415", "请求媒体类型不受支持", "{\n  \"error\": {\n    \"status\": 415,\n    \"reason\": \"unsupported_media_type\",\n    \"message\": \"request content type is not supported\",\n    \"request_id\": \"req_01HXYZ\"\n  }\n}");
        }
        if (supportsPayloadTooLarge(path)) {
            addErrorResponse(operation.getResponses(), "413", "请求体或上传内容超过服务端限制", "{\n  \"error\": {\n    \"status\": 413,\n    \"reason\": \"payload_too_large\",\n    \"message\": \"request payload is too large\",\n    \"request_id\": \"req_01HXYZ\"\n  }\n}");
        }
        if (supportsServiceUnavailable(path)) {
            boolean mailRoute = "/api/auth/email_codes".equals(path);
            String reason = mailRoute ? "mail_service_unavailable" : "storage_service_unavailable";
            String message = mailRoute ? "mail service is unavailable" : "storage service is unavailable";
            addErrorResponse(operation.getResponses(), "503", "依赖的外部服务不可用", "{\n  \"error\": {\n    \"status\": 503,\n    \"reason\": \""
                    + reason + "\",\n    \"message\": \"" + message + "\",\n    \"request_id\": \"req_01HXYZ\"\n  }\n}");
            if (mailRoute) {
                operation.getResponses().get("503").getContent().get(JSON_MEDIA_TYPE)
                        .addExamples("delivery_failed", new Example()
                                .description("503")
                                .summary("邮件投递失败")
                                .value("{\n  \"error\": {\n    \"status\": 503,\n    \"reason\": \"email_delivery_failed\",\n    \"message\": \"failed to deliver verification email\",\n    \"request_id\": \"req_01HXYZ\"\n  }\n}"));
            }
        }
    }

    private static void addErrorResponse(ApiResponses responses, String status, String description, String jsonValue) {
        ApiResponse response = new ApiResponse()
                .description(description)
                .content(new Content().addMediaType(JSON_MEDIA_TYPE, new MediaType()
                        .addExamples("error", new Example()
                                .description(status)
                                .summary(description)
                                .value(jsonValue))));
        ApiResponse existing = responses.get(status);
        if (existing != null) {
            if (existing.getDescription() == null || existing.getDescription().isBlank()) {
                existing.setDescription(description);
            }
            if (existing.getContent() == null
                    || existing.getContent().isEmpty()
                    || existing.getContent().containsKey("*/*")) {
                existing.setContent(response.getContent());
            }
            if ("401".equals(status)) {
                existing.addHeaderObject("WWW-Authenticate", bearerChallengeHeader());
            }
            return;
        }
        if ("401".equals(status)) {
            response.addHeaderObject("WWW-Authenticate", bearerChallengeHeader());
        }
        responses.addApiResponse(status, response);
    }

    private static Header bearerChallengeHeader() {
        return new Header()
                .description("Bearer authentication challenge")
                .schema(new StringSchema().example("Bearer"));
    }

    private static boolean supportsNotFound(String path) {
        if (path == null) {
            return false;
        }
        return path.startsWith("/api/users/")
                || path.startsWith("/api/channels/")
                || path.startsWith("/api/messages/")
                || path.startsWith("/api/server/");
    }

    private static boolean supportsUnauthorized(String path) {
        return OpenApiSecurityPolicy.requiresAuthentication(path)
                || "/api/auth/login".equals(path)
                || "/api/auth/refresh".equals(path)
                || "/api/auth/revoke".equals(path);
    }

    private static boolean supportsConflict(String path, String httpMethod) {
        if (path == null) {
            return false;
        }
        return ("POST".equals(httpMethod) && (path.contains("/forward") || path.contains("/pins/")))
                || ("DELETE".equals(httpMethod) && "/api/channels/{channelId}".equals(path))
                || ("POST".equals(httpMethod)
                && "/api/channels/{channelId}/applications/{applicationId}/decisions".equals(path));
    }

    private static boolean supportsPayloadTooLarge(String path) {
        if (path == null) {
            return false;
        }
        return "/api/users/me/background".equals(path)
                || path.contains("/files/uploads/")
                || path.endsWith("/messages/attachments");
    }

    private static boolean supportsServiceUnavailable(String path) {
        if (path == null) {
            return false;
        }
        return "/api/auth/email_codes".equals(path)
                || "/api/users/me/background".equals(path)
                || "/api/files/uploads/{shareKey}".equals(path)
                || "/api/files/download/{shareKey}".equals(path)
                || "/api/channels/{channelId}/messages/attachments".equals(path);
    }
}
