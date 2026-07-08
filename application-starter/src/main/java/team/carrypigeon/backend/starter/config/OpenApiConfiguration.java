package team.carrypigeon.backend.starter.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.Map;
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
    private static final String MULTIPART_FORM_DATA_TYPE = "multipart/form-data";
    private static final String OCTET_STREAM_TYPE = "application/octet-stream";
    private static final Map<String, RequestExample> REQUEST_EXAMPLES = Map.ofEntries(
            Map.entry("POST /api/auth/login", jsonExample(
                    "测试账号登录",
                    """
                            {
                              "username": "carry-owner",
                              "password": "carrypigeon123"
                            }
                            """
            )),
            Map.entry("POST /api/auth/register", jsonExample(
                    "用户名密码注册",
                    """
                            {
                              "username": "carry-user",
                              "password": "carrypigeon123"
                            }
                            """
            )),
            Map.entry("POST /api/auth/email_codes", jsonExample(
                    "发送邮箱验证码",
                    """
                            {
                              "email": "carry-user@example.test"
                            }
                            """
            )),
            Map.entry("POST /api/auth/tokens", jsonExample(
                    "邮箱验证码登录",
                    """
                            {
                              "grant_type": "email_code",
                              "email": "carry-user@example.test",
                              "code": "123456",
                              "client": {
                                "device_id": "apifox-device-1",
                                "installed_plugins": []
                              }
                            }
                            """
            )),
            Map.entry("POST /api/auth/refresh", jsonExample(
                    "刷新访问令牌",
                    """
                            {
                              "refresh_token": "{{refreshToken}}",
                              "client": {
                                "device_id": "apifox-device-1"
                              }
                            }
                            """
            )),
            Map.entry("POST /api/auth/revoke", jsonExample(
                    "撤销刷新令牌",
                    """
                            {
                              "refresh_token": "{{refreshToken}}",
                              "client": {
                                "device_id": "apifox-device-1"
                              }
                            }
                            """
            )),
            Map.entry("POST /api/gates/required/check", jsonExample(
                    "required gate 预检查",
                    """
                            {
                              "client": {
                                "device_id": "apifox-device-1",
                                "installed_plugins": []
                              }
                            }
                            """
            )),
            Map.entry("PUT /api/users/me/email", jsonExample(
                    "更新邮箱",
                    """
                            {
                              "email": "carry-user@example.test",
                              "code": "123456"
                            }
                            """
            )),
            Map.entry("PATCH /api/users/me", jsonExample(
                    "更新当前用户资料",
                    """
                            {
                              "username": "carry-owner",
                              "avatar": "avatars/users/carry-owner.png",
                              "brief": "Updated from Apifox",
                              "sex": 0,
                              "birthday": 0
                            }
                            """
            )),
            Map.entry("POST /api/users/me/background", formExample(
                    "上传用户背景图",
                    Map.of("background", "<select image file>")
            )),
            Map.entry("POST /api/files/uploads", jsonExample(
                    "申请文件上传",
                    """
                            {
                              "filename": "apifox-note.txt",
                              "mime_type": "text/plain",
                              "size_bytes": 128
                            }
                            """
            )),
            Map.entry("PUT /api/files/uploads/{shareKey}", binaryExample(
                    "上传文件内容",
                    "<raw file content>"
            )),
            Map.entry("POST /api/channels", jsonExample(
                    "创建频道",
                    """
                            {
                              "name": "apifox-channel",
                              "brief": "Created from Apifox",
                              "avatar": "avatars/channels/apifox-channel.png"
                            }
                            """
            )),
            Map.entry("PATCH /api/channels/{channelId}", jsonExample(
                    "更新频道资料",
                    """
                            {
                              "name": "project-alpha",
                              "brief": "Updated from Apifox"
                            }
                            """
            )),
            Map.entry("PUT /api/channels/{channelId}/bans/{targetAccountId}", jsonExample(
                    "禁言频道成员",
                    """
                            {
                              "reason": "spam",
                              "until": 1893456000000
                            }
                            """
            )),
            Map.entry("PUT /api/channels/{channelId}/notification_preference", jsonExample(
                    "更新频道通知偏好",
                    """
                            {
                              "mode": "inherit",
                              "muted_until": 0
                            }
                            """
            )),
            Map.entry("POST /api/channels/{channelId}/applications", jsonExample(
                    "申请加入频道",
                    """
                            {
                              "reason": "I want to join this channel"
                            }
                            """
            )),
            Map.entry("POST /api/channels/{channelId}/applications/{applicationId}/decisions", jsonExample(
                    "审批入群申请",
                    """
                            {
                              "decision": "approve"
                            }
                            """
            )),
            Map.entry("POST /api/channels/{channelId}/messages", jsonExample(
                    "发送文本消息",
                    """
                            {
                              "domain": "Core:Text",
                              "domain_version": "1.0.0",
                              "data": {
                                "text": "hello from Apifox"
                              },
                              "reply_to_mid": null,
                              "mentions": [],
                              "client_message_id": "apifox-msg-001"
                            }
                            """
            )),
            Map.entry("POST /api/channels/{channelId}/messages/attachments", formExample(
                    "上传消息附件",
                    Map.of(
                            "message_type", "file",
                            "file", "<select attachment file>"
                    )
            )),
            Map.entry("POST /api/channels/{channelId}/pins/{messageId}", jsonExample(
                    "置顶频道消息",
                    """
                            {
                              "note": "Important message"
                            }
                            """
            )),
            Map.entry("PATCH /api/messages/{messageId}", jsonExample(
                    "编辑文本消息",
                    """
                            {
                              "domain": "Core:Text",
                              "domain_version": "1.0.0",
                              "data": {
                                "text": "edited from Apifox"
                              },
                              "mentions": [],
                              "expected_edit_version": 1
                            }
                            """
            )),
            Map.entry("POST /api/messages/{messageId}/forward", jsonExample(
                    "转发消息",
                            """
                            {
                              "target_cid": "{{channelId}}",
                              "comment": "FYI",
                              "idempotency_key": "apifox-forward-001"
                            }
                            """
            )),
            Map.entry("PUT /api/channels/{channelId}/read_state", jsonExample(
                    "更新频道已读状态",
                            """
                            {
                              "last_read_mid": "{{messageId}}",
                              "last_read_time": 1700000000000
                            }
                            """
            )),
            Map.entry("PUT /api/mentions/read_state", jsonExample(
                    "批量标记提及已读",
                            """
                            {
                              "before_mention_id": "{{mentionId}}",
                              "cid": "{{channelId}}"
                            }
                            """
            )),
            Map.entry("PUT /api/notification_preferences/server", jsonExample(
                    "更新服务通知偏好",
                    """
                            {
                              "mode": "all",
                              "muted_until": null
                            }
                            """
            ))
    );

    /**
     * 注册当前服务的 OpenAPI 文档模型。
     *
     * @return 包含基础文档元数据与 Bearer 鉴权声明的 OpenAPI 对象
     */
    @Bean
    public OpenAPI carryPigeonOpenApi() {
        OpenAPI openAPI = new OpenAPI()
                .info(new Info()
                        .title("CarryPigeon Backend OpenAPI Portal")
                        .version("v1")
                        .description("面向前端、测试与集成方的 CarryPigeon Backend HTTP API 门户。\n\n"
                                + "使用说明：\n"
                                + "1. 大多数受保护接口需要在 Swagger Authorize 中填写 `Bearer <access-token>`。\n"
                                + "2. 当前对外协议以 `docs/t` 下的 v1 HTTP/WS 规范为基准，HTTP 成功响应直接返回资源对象，不使用 `CPResponse` 统一成功包装。\n"
                                + "3. JSON 字段统一为 `snake_case`，雪花 ID 统一编码为十进制字符串。\n"
                                + "4. 失败响应统一为 `{ \"error\": { status, reason, message, details? } }`，请以 `error.reason` 作为分支条件。\n"
                                + "5. Apifox 导入建议使用 `/v3/api-docs`；导入后创建 `local` 环境，并配置 `token`、`refreshToken`、`channelId=100` 等变量。"))
                .addServersItem(new Server()
                        .url("http://127.0.0.1:8080")
                        .description("Local Spring Boot HTTP server"))
                .components(new Components().addSecuritySchemes(
                        AUTH_SCHEME_NAME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                ));
        openAPI.addExtension("x-apifox-import-note", Map.ofEntries(
                Map.entry("base_url", "http://127.0.0.1:8080"),
                Map.entry("websocket_url", "ws://127.0.0.1:18080/api/ws"),
                Map.entry("websocket_enabled_config", "CP_CHAT_REALTIME_ENABLED=true (default); set false to disable"),
                Map.entry("login_username", "carry-owner"),
                Map.entry("login_password", "carrypigeon123"),
                Map.entry("default_channel_id", "100"),
                Map.entry("token_variable", "token"),
                Map.entry("refresh_token_variable", "refreshToken"),
                Map.entry("channel_id_variable", "channelId"),
                Map.entry("message_id_variable", "messageId"),
                Map.entry("mention_id_variable", "mentionId"),
                Map.entry("share_key_variable", "shareKey"),
                Map.entry("target_account_id_variable", "targetAccountId"),
                Map.entry("application_id_variable", "applicationId")
        ));
        return openAPI;
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
                pathItem.readOperationsMap().forEach((httpMethod, operation) -> {
                    if (operation == null) {
                        return;
                    }

                    if (requiresAuthentication(path)
                            && (operation.getSecurity() == null || operation.getSecurity().isEmpty())) {
                        operation.addSecurityItem(new SecurityRequirement().addList(AUTH_SCHEME_NAME));
                    }

                    ensureRequestExample(operation, httpMethod.name(), path);
                    ensureCommonErrorResponses(operation, path);
                });
            });
        };
    }

    private static void ensureRequestExample(io.swagger.v3.oas.models.Operation operation, String method, String path) {
        RequestExample requestExample = REQUEST_EXAMPLES.get(method + " " + path);
        if (requestExample == null) {
            return;
        }
        RequestBody requestBody = operation.getRequestBody();
        if (requestBody == null) {
            requestBody = new RequestBody();
            operation.setRequestBody(requestBody);
        }
        if (requestBody.getContent() == null) {
            requestBody.setContent(new Content());
        }
        MediaType mediaType = requestBody.getContent().get(requestExample.mediaType());
        if (mediaType == null) {
            mediaType = new MediaType();
            requestBody.getContent().addMediaType(requestExample.mediaType(), mediaType);
        }
        if (mediaType.getExamples() == null || !mediaType.getExamples().containsKey(requestExample.name())) {
            mediaType.addExamples(requestExample.name(), example(requestExample.name(), requestExample.name(), requestExample.value()));
        }
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

    private static Example example(String name, String summary, Object value) {
        return new Example()
                .description(name)
                .summary(summary)
                .value(value);
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
            && !"/api/auth/revoke".equals(path);
    }

    private static RequestExample jsonExample(String name, String jsonValue) {
        return new RequestExample(name, JSON_MEDIA_TYPE, jsonValue);
    }

    private static RequestExample formExample(String name, Map<String, String> value) {
        return new RequestExample(name, MULTIPART_FORM_DATA_TYPE, value);
    }

    private static RequestExample binaryExample(String name, String value) {
        return new RequestExample(name, OCTET_STREAM_TYPE, value);
    }

    private record RequestExample(String name, String mediaType, Object value) {
    }
}
