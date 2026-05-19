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
                                + "2. 当前接口通常返回 HTTP 200，请结合 `CPResponse.code` 判断真实业务结果。\n"
                                + "3. `code=100` 表示成功；`200/300/404/500` 分别表示参数错误、认证或权限失败、资源不存在、内部错误。\n"
                                + "4. 文档中的 success / validation_failed / forbidden / not_found / internal_error 示例，展示的是同一 HTTP 200 包装下的不同业务结果。"))
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
                    ensureKeySuccessExamples(operation, path);
                });
            });
        };
    }

    private static void ensureCommonErrorResponses(io.swagger.v3.oas.models.Operation operation, String path) {
        if (operation.getResponses() == null) {
            operation.setResponses(new ApiResponses());
        }

        operation.getResponses().addApiResponse("200", commonBusinessResponse(path));
    }

    private static boolean supportsNotFound(String path) {
        if (path == null) {
            return false;
        }

        return path.startsWith("/api/users/")
                || path.startsWith("/api/channels/")
                || "/api/channels/default".equals(path)
                || "/api/channels/system".equals(path)
                || "/api/server/presence/me".equals(path);
    }

    private static ApiResponse commonBusinessResponse(String path) {
        MediaType mediaType = new MediaType()
                .addExamples("success", example(
                        "success",
                        "成功响应",
                        "{\n  \"code\": 100,\n  \"message\": \"success\",\n  \"data\": {}\n}"
                ))
                .addExamples("validation_failed", example(
                        "validation_failed",
                        "参数错误",
                        "{\n  \"code\": 200,\n  \"message\": \"limit must be between 1 and 100\",\n  \"data\": null\n}"
                ))
                .addExamples("internal_error", example(
                        "internal_error",
                        "服务内部错误",
                        "{\n  \"code\": 500,\n  \"message\": \"internal server error\",\n  \"data\": null\n}"
                ));

        if (requiresAuthentication(path)) {
            mediaType.addExamples("forbidden", example(
                    "forbidden",
                    "认证或权限失败",
                    "{\n  \"code\": 300,\n  \"message\": \"authentication is required\",\n  \"data\": null\n}"
            ));
        }

        if (supportsNotFound(path)) {
            mediaType.addExamples("not_found", example(
                    "not_found",
                    "资源不存在",
                    "{\n  \"code\": 404,\n  \"message\": \"channel does not exist\",\n  \"data\": null\n}"
            ));
        }

        return new ApiResponse()
                .description("当前接口通常返回 HTTP 200；请读取 `CPResponse.code` 判断业务成功或失败。常见业务码包括 100=成功、200=参数错误、300=认证或权限失败、404=资源不存在、500=内部错误。")
                .content(new Content().addMediaType(JSON_MEDIA_TYPE, mediaType));
    }

    private static void ensureKeySuccessExamples(io.swagger.v3.oas.models.Operation operation, String path) {
        if (operation.getResponses() == null) {
            operation.setResponses(new ApiResponses());
        }

        ApiResponse success = operation.getResponses().get("200");
        if (success == null) {
            success = new ApiResponse().description("HTTP 状态通常为 200；请读取 `CPResponse.code` 判断业务成功或失败。常见业务码包括 100=成功、200=参数错误、300=认证或权限失败、404=资源不存在、500=内部错误");
            operation.getResponses().addApiResponse("200", success);
        }

        Content content = success.getContent();
        if (content == null) {
            content = new Content();
            success.setContent(content);
        }

        MediaType mediaType = content.get(JSON_MEDIA_TYPE);
        if (mediaType == null) {
            mediaType = new MediaType();
            content.addMediaType(JSON_MEDIA_TYPE, mediaType);
        }

        addSuccessExamples(mediaType, path);
    }

    private static void addSuccessExamples(MediaType mediaType, String path) {
        if (path == null) {
            return;
        }

        if ("/api/auth/register".equals(path)) {
            mediaType.addExamples("register_success", example(
                    "register_success",
                    "注册成功",
                    "{\n  \"code\": 100,\n  \"message\": \"success\",\n  \"data\": {\n    \"accountId\": 1001,\n    \"username\": \"carry_user\"\n  }\n}"
            ));
        } else if ("/api/auth/login".equals(path) || "/api/auth/refresh".equals(path)) {
            mediaType.addExamples("token_success", example(
                    "token_success",
                    "令牌获取成功",
                    "{\n  \"code\": 100,\n  \"message\": \"success\",\n  \"data\": {\n    \"accountId\": 1001,\n    \"username\": \"carry_user\",\n    \"accessToken\": \"eyJhbGciOiJIUzI1NiJ9.access.token\",\n    \"accessTokenExpiresAt\": \"2026-05-14T12:00:00Z\",\n    \"refreshToken\": \"eyJhbGciOiJIUzI1NiJ9.refresh.token\",\n    \"refreshTokenExpiresAt\": \"2026-05-28T12:00:00Z\"\n  }\n}"
            ));
        } else if ("/api/auth/logout".equals(path)) {
            mediaType.addExamples("logout_success", example(
                    "logout_success",
                    "注销成功",
                    "{\n  \"code\": 100,\n  \"message\": \"success\",\n  \"data\": null\n}"
            ));
        } else if ("/api/auth/me".equals(path)) {
            mediaType.addExamples("current_user_success", example(
                    "current_user_success",
                    "当前用户查询成功",
                    "{\n  \"code\": 100,\n  \"message\": \"success\",\n  \"data\": {\n    \"accountId\": 1001,\n    \"username\": \"carry_user\"\n  }\n}"
            ));
        } else if ("/api/server/echo".equals(path)) {
            mediaType.addExamples("echo_success", example(
                    "echo_success",
                    "回显成功",
                    "{\n  \"code\": 100,\n  \"message\": \"success\",\n  \"data\": \"pong\"\n}"
            ));
        } else if ("/api/server/presence/me".equals(path)) {
            mediaType.addExamples("presence_success", example(
                    "presence_success",
                    "Presence 查询成功",
                    "{\n  \"code\": 100,\n  \"message\": \"success\",\n  \"data\": {\n    \"account_id\": 1001,\n    \"status\": \"ONLINE\",\n    \"online_session_count\": 1\n  }\n}"
            ));
        } else if ("/api/users/me".equals(path) || "/api/users/{accountId}".equals(path)) {
            mediaType.addExamples("user_profile_success", example(
                    "user_profile_success",
                    "用户资料查询成功",
                    "{\n  \"code\": 100,\n  \"message\": \"success\",\n  \"data\": {\n    \"accountId\": 1001,\n    \"nickname\": \"Carry Pigeon\",\n    \"avatarUrl\": \"https://cdn.example.com/avatar.png\",\n    \"bio\": \"Backend developer and pigeon lover\",\n    \"createdAt\": \"2026-05-01T08:00:00Z\",\n    \"updatedAt\": \"2026-05-13T08:00:00Z\"\n  }\n}"
            ));
        } else if ("/api/users".equals(path)) {
            mediaType.addExamples("user_profile_list_success", example(
                    "user_profile_list_success",
                    "用户资料列表成功",
                    "{\n  \"code\": 100,\n  \"message\": \"success\",\n  \"data\": [\n    {\n      \"account_id\": 1001,\n      \"nickname\": \"Carry Pigeon\",\n      \"avatar_url\": \"https://cdn.example.com/avatar.png\",\n      \"bio\": \"Backend developer and pigeon lover\",\n      \"created_at\": \"2026-05-01T08:00:00Z\",\n      \"updated_at\": \"2026-05-13T08:00:00Z\"\n    }\n  ]\n}"
            ));
        } else if ("/api/users/page".equals(path) || "/api/users/search".equals(path)) {
            mediaType.addExamples("user_profile_page_success", example(
                    "user_profile_page_success",
                    "用户资料分页成功",
                    "{\n  \"code\": 100,\n  \"message\": \"success\",\n  \"data\": {\n    \"users\": [\n      {\n        \"account_id\": 1001,\n        \"nickname\": \"Carry Pigeon\",\n        \"avatar_url\": \"https://cdn.example.com/avatar.png\",\n        \"bio\": \"Backend developer and pigeon lover\",\n        \"created_at\": \"2026-05-01T08:00:00Z\",\n        \"updated_at\": \"2026-05-13T08:00:00Z\"\n      }\n    ],\n    \"next_cursor\": 1000\n  }\n}"
            ));
        } else if ("/api/channels/default".equals(path) || "/api/channels/system".equals(path) || "/api/channels/private".equals(path)) {
            mediaType.addExamples("channel_success", example(
                    "channel_success",
                    "频道查询/创建成功",
                    "{\n  \"code\": 100,\n  \"message\": \"success\",\n  \"data\": {\n    \"channel_id\": 2001,\n    \"conversation_id\": 3001,\n    \"name\": \"Project Phoenix\",\n    \"type\": \"private\",\n    \"default_channel\": false,\n    \"created_at\": \"2026-05-01T08:00:00Z\",\n    \"updated_at\": \"2026-05-13T08:00:00Z\"\n  }\n}"
            ));
        }
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

        return !"/api/auth/register".equals(path)
                && !"/api/auth/login".equals(path)
                && !"/api/auth/refresh".equals(path)
                && !"/api/auth/logout".equals(path)
                && !"/api/server/echo".equals(path);
    }
}
