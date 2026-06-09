package team.carrypigeon.backend.starter.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 文档装配配置。
 * 职责：在 starter 层注册当前 HTTP API 的最小 OpenAPI 元数据与 Bearer 鉴权方案。
 * 边界：这里只负责文档装配，不改变现有控制器的业务语义与鉴权拦截规则。
 */
@Configuration
public class OpenApiConfiguration {

    private static final String AUTH_SCHEME_NAME = "bearerAuth";
    private static final String JSON_MEDIA_TYPE = "application/json";

    /**
     * 注册当前服务的 OpenAPI 文档模型。
     *
     * @return 包含基础文档元数据与 Bearer 鉴权声明的 OpenAPI 对象
     */
    @Bean
    public OpenAPI carryPigeonOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("CarryPigeon Backend OpenAPI Portal")
                        .version("v1")
                        .description("面向前端、测试与集成方的 CarryPigeon Backend HTTP API 门户。\n\n"
                                + "使用说明：\n"
                                + "1. 大多数受保护接口需要在 Swagger Authorize 中填写 `Bearer <access-token>`。\n"
                                + "2. 当前对外协议以 `docs/t` 下的 v1 HTTP/WS 规范为基准，HTTP 成功响应直接返回资源对象，不使用 `CPResponse` 统一成功包装。\n"
                                + "3. JSON 字段统一为 `snake_case`，雪花 ID 统一编码为十进制字符串。\n"
                                + "4. 失败响应统一为 `{ \"error\": { status, reason, message, details? } }`，请以 `error.reason` 作为分支条件。"))
                .components(new Components().addSecuritySchemes(
                        AUTH_SCHEME_NAME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                ));
    }

    /**
     * 根据当前 HTTP 入口实际认证要求补充 OpenAPI 鉴权声明。
     *
     * @return 用于在生成后的 OpenAPI 文档上标记受保护操作的自定义器
     */
    @Bean
    public OpenApiCustomizer securedApiOperationCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }

            openApi.getPaths().forEach((path, pathItem) -> {
                pathItem.readOperations().forEach(operation -> {
                    if (operation == null) {
                        return;
                    }

                    if (requiresAuthentication(path)
                            && (operation.getSecurity() == null || operation.getSecurity().isEmpty())) {
                        operation.addSecurityItem(new SecurityRequirement().addList(AUTH_SCHEME_NAME));
                    }

                    ensureCommonErrorResponses(operation, path);
                });
            });
        };
    }

    private static void ensureCommonErrorResponses(io.swagger.v3.oas.models.Operation operation, String path) {
        if (operation.getResponses() == null) {
            operation.setResponses(new ApiResponses());
        }

        addErrorResponse(operation.getResponses(), "422", "请求参数或请求体校验失败", "{\n  \"error\": {\n    \"status\": 422,\n    \"reason\": \"validation_failed\",\n    \"message\": \"validation failed\",\n    \"request_id\": \"req_01HXYZ\",\n    \"details\": {\n      \"field_errors\": [\n        {\n          \"field\": \"limit\",\n          \"reason\": \"invalid\",\n          \"message\": \"limit must be between 1 and 50\"\n        }\n      ]\n    }\n  }\n}");
        addErrorResponse(operation.getResponses(), "500", "服务端内部错误", "{\n  \"error\": {\n    \"status\": 500,\n    \"reason\": \"internal_error\",\n    \"message\": \"internal server error\",\n    \"request_id\": \"req_01HXYZ\"\n  }\n}");

        if (requiresAuthentication(path)) {
            addErrorResponse(operation.getResponses(), "401", "缺少或无效的 access token", "{\n  \"error\": {\n    \"status\": 401,\n    \"reason\": \"unauthorized\",\n    \"message\": \"authentication is required\",\n    \"request_id\": \"req_01HXYZ\"\n  }\n}");
            addErrorResponse(operation.getResponses(), "403", "已认证但无权执行该操作", "{\n  \"error\": {\n    \"status\": 403,\n    \"reason\": \"forbidden\",\n    \"message\": \"forbidden\",\n    \"request_id\": \"req_01HXYZ\"\n  }\n}");
        }

        if (supportsNotFound(path)) {
            addErrorResponse(operation.getResponses(), "404", "目标资源不存在", "{\n  \"error\": {\n    \"status\": 404,\n    \"reason\": \"not_found\",\n    \"message\": \"resource does not exist\",\n    \"request_id\": \"req_01HXYZ\"\n  }\n}");
        }

        if ("/api/auth/tokens".equals(path)) {
            addErrorResponse(operation.getResponses(), "412", "required gate 未满足", "{\n  \"error\": {\n    \"status\": 412,\n    \"reason\": \"required_plugin_missing\",\n    \"message\": \"required plugins are missing\",\n    \"request_id\": \"req_01HXYZ\",\n    \"details\": {\n      \"missing_plugins\": [\"mc-bind\"]\n    }\n  }\n}");
        }

        if (supportsConflict(path)) {
            addErrorResponse(operation.getResponses(), "409", "资源状态冲突", "{\n  \"error\": {\n    \"status\": 409,\n    \"reason\": \"conflict\",\n    \"message\": \"resource state conflict\",\n    \"request_id\": \"req_01HXYZ\"\n  }\n}");
        }
    }

    private static void addErrorResponse(ApiResponses responses, String status, String description, String jsonValue) {
        if (responses.get(status) != null) {
            return;
        }
        responses.addApiResponse(status, new ApiResponse()
                .description(description)
                .content(new Content().addMediaType(JSON_MEDIA_TYPE, new MediaType()
                        .addExamples("error", example(status, description, jsonValue)))));
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

    private static boolean supportsConflict(String path) {
        if (path == null) {
            return false;
        }
        return path.startsWith("/api/messages/")
                || path.contains("/pins")
                || path.contains("/forward");
    }

    private static Example example(String name, String summary, String jsonValue) {
        return new Example()
                .description(name)
                .summary(summary)
                .value(jsonValue);
    }

    private static boolean requiresAuthentication(String path) {
        if (path == null || !path.startsWith("/api/")) {
            return false;
        }

        if ("/api/server".equals(path) || "/api/gates/required/check".equals(path) || "/api/plugins/catalog".equals(path) || "/api/domains/catalog".equals(path)) {
            return false;
        }

        return !"/api/auth/register".equals(path)
                && !"/api/auth/login".equals(path)
                && !"/api/auth/email_codes".equals(path)
                && !"/api/auth/tokens".equals(path)
                && !"/api/auth/refresh".equals(path)
                && !"/api/auth/revoke".equals(path)
                && !"/api/server".equals(path)
                && !"/api/gates/required/check".equals(path);
    }
}
