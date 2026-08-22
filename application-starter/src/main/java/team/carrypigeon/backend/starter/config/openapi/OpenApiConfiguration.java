package team.carrypigeon.backend.starter.config.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.Map;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 文档装配配置。
 * 职责：注册文档元数据，并组合请求示例、安全声明与错误响应定制器。
 * 边界：不改变控制器业务语义和实际鉴权规则。
 */
@Configuration
public class OpenApiConfiguration {

    static final String AUTH_SCHEME_NAME = "bearerAuth";

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
                                + "1. 大多数受保护接口需要在 Swagger Authorize 中仅填写 access token，Bearer 前缀由 Swagger UI 添加。\n"
                                + "2. 当前对外协议以 `docs/api/API.md` 为基准，HTTP 成功响应直接返回资源对象，不使用 `CPResponse` 统一成功包装。\n"
                                + "3. JSON 字段、查询参数与 path 参数统一为 `snake_case`，雪花 ID 统一编码为十进制字符串。\n"
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
                Map.entry("websocket_enabled_config", "cp.chat.server.realtime.enabled=true in config/application.yaml; set false to disable"),
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
     * 注册按操作补充文档细节的组合定制器。
     *
     * @return OpenAPI 操作定制器
     */
    @Bean
    public OpenApiCustomizer securedApiOperationCustomizer() {
        return openApi -> {
            OpenApiSchemaNamingPolicy.normalize(openApi);
            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().forEach((path, pathItem) -> pathItem.readOperationsMap()
                    .forEach((httpMethod, operation) -> {
                        if (operation == null) {
                            return;
                        }
                        OpenApiSecurityPolicy.ensureSecurity(operation, path);
                        OpenApiRequestExamples.ensureRequestExample(operation, httpMethod.name(), path);
                        OpenApiErrorResponses.ensureCommonErrorResponses(operation, path, httpMethod.name());
                    }));
            OpenApiParameterNamingPolicy.normalize(openApi);
        };
    }
}
