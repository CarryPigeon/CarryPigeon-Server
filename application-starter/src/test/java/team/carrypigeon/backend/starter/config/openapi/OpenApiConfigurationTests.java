package team.carrypigeon.backend.starter.config.openapi;

import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import team.carrypigeon.backend.chat.domain.features.auth.controller.http.CurrentUserAccountController;
import team.carrypigeon.backend.chat.domain.features.channel.controller.http.AuditLogController;
import team.carrypigeon.backend.chat.domain.features.channel.controller.http.ChannelLifecycleController;
import team.carrypigeon.backend.chat.domain.features.channel.controller.http.ChannelMemberGovernanceController;
import team.carrypigeon.backend.chat.domain.features.server.controller.http.ChannelNotificationPreferenceController;
import team.carrypigeon.backend.chat.domain.features.file.controller.http.FileController;
import team.carrypigeon.backend.chat.domain.features.message.controller.http.ChannelMessageController;
import team.carrypigeon.backend.chat.domain.features.message.controller.http.ChannelPinsController;
import team.carrypigeon.backend.chat.domain.features.message.controller.http.MessageController;
import team.carrypigeon.backend.chat.domain.features.server.controller.http.NotificationPreferenceController;
import team.carrypigeon.backend.chat.domain.features.user.controller.http.UserProfileController;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OpenAPI 文档装配测试。
 * 职责：验证 starter 层会注册最小 OpenAPI 文档模型与 Bearer 鉴权方案。
 * 边界：只验证配置 Bean 契约，不覆盖 springdoc 自身框架行为。
 */
@Tag("contract")
class OpenApiConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(OpenApiConfiguration.class);

    /**
     * 验证 OpenAPI 配置会注册包含基础信息和 Bearer 鉴权方案的文档 Bean。
     * 输入：仅加载 OpenAPI 配置类。
     * 输出：上下文存在 OpenAPI Bean，且 Bearer 鉴权方案命名稳定。
     */
    @Test
    @DisplayName("configuration registers openapi bean with bearer scheme")
    void configuration_registersOpenApiBeanWithBearerScheme() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(OpenAPI.class);

            OpenAPI openAPI = context.getBean(OpenAPI.class);
            assertThat(openAPI.getInfo()).isNotNull();
            assertThat(openAPI.getInfo().getTitle()).isEqualTo("CarryPigeon Backend OpenAPI Portal");
            assertThat(openAPI.getInfo().getVersion()).isEqualTo("v1");
            assertThat(openAPI.getServers())
                    .singleElement()
                    .satisfies(server -> {
                        assertThat(server.getUrl()).isEqualTo("http://127.0.0.1:8080");
                        assertThat(server.getDescription()).isEqualTo("Local Spring Boot HTTP server");
                    });
            assertThat(openAPI.getExtensions())
                    .containsKey("x-apifox-import-note");
            @SuppressWarnings("unchecked")
            Map<String, Object> apifoxNote = (Map<String, Object>) openAPI.getExtensions().get("x-apifox-import-note");
            assertThat(apifoxNote)
                    .containsEntry("websocket_url", "ws://127.0.0.1:18080/api/ws")
                    .containsEntry("websocket_enabled_config", "cp.chat.server.realtime.enabled=true in config/application.yaml; set false to disable")
                    .containsEntry("token_variable", "token")
                    .containsEntry("channel_id_variable", "channelId")
                    .containsEntry("message_id_variable", "messageId")
                    .containsEntry("mention_id_variable", "mentionId");
            assertThat(openAPI.getComponents()).isNotNull();
            assertThat(openAPI.getComponents().getSecuritySchemes()).containsKey("bearerAuth");

            SecurityScheme bearerAuth = openAPI.getComponents().getSecuritySchemes().get("bearerAuth");
            assertThat(bearerAuth.getType()).isEqualTo(SecurityScheme.Type.HTTP);
            assertThat(bearerAuth.getScheme()).isEqualTo("bearer");
            assertThat(bearerAuth.getBearerFormat()).isEqualTo("JWT");
        });
    }

    /**
     * 验证 OpenAPI 自定义器会按当前拦截规则只为受保护 `/api/**` 操作追加 Bearer 鉴权声明。
     * 输入：包含匿名与受保护路径的最小 OpenAPI 路径集合。
     * 输出：仅受保护操作带有 `bearerAuth` 安全声明。
     */
    @Test
    @DisplayName("customizer marks protected api operations with bearer auth")
    void customizer_marksProtectedApiOperationsWithBearerAuth() {
        contextRunner.run(context -> {
            OpenApiCustomizer customizer = context.getBean(OpenApiCustomizer.class);

            Operation protectedOperation = new Operation();
            Operation publicRegisterOperation = new Operation();
            Operation publicLoginOperation = new Operation();
            Operation publicTokenOperation = new Operation();
            Operation publicServerOperation = new Operation();
            Operation publicPluginCatalogOperation = new Operation();
            Operation publicDomainCatalogOperation = new Operation();
            Operation publicGateOperation = new Operation();
            Operation protectedChannelOperation = new Operation();

            OpenAPI openAPI = new OpenAPI().paths(new Paths()
                    .addPathItem("/api/users/me", new PathItem().get(protectedOperation))
                    .addPathItem("/api/auth/register", new PathItem().post(publicRegisterOperation))
                    .addPathItem("/api/auth/login", new PathItem().post(publicLoginOperation))
                    .addPathItem("/api/auth/tokens", new PathItem().post(publicTokenOperation))
                    .addPathItem("/api/server", new PathItem().get(publicServerOperation))
                    .addPathItem("/api/plugins/catalog", new PathItem().get(publicPluginCatalogOperation))
                    .addPathItem("/api/domains/catalog", new PathItem().get(publicDomainCatalogOperation))
                    .addPathItem("/api/gates/required/check", new PathItem().post(publicGateOperation))
                    .addPathItem("/api/channels/1/messages", new PathItem().get(protectedChannelOperation))
            );

            customizer.customise(openAPI);

            assertThat(protectedOperation.getSecurity())
                    .isNotNull()
                    .singleElement()
                    .satisfies(requirement -> assertThat(requirement).containsKey("bearerAuth"));
            assertThat(publicRegisterOperation.getSecurity()).isNull();
            assertThat(publicLoginOperation.getSecurity()).isNull();
            assertThat(publicTokenOperation.getSecurity()).isNull();
            assertThat(publicServerOperation.getSecurity()).isNull();
            assertThat(publicPluginCatalogOperation.getSecurity()).isNull();
            assertThat(publicDomainCatalogOperation.getSecurity()).isNull();
            assertThat(publicGateOperation.getSecurity()).isNull();
            assertThat(protectedChannelOperation.getSecurity())
                    .isNotNull()
                    .singleElement()
                    .satisfies(requirement -> assertThat(requirement).containsKey("bearerAuth"));
        });
    }

    /**
     * 验证 OpenAPI 自定义器会把 schema 属性、嵌套属性和必填列表统一为 snake_case。
     * 输入：包含 camelCase 属性、数组 items 和 required 声明的组件 schema。
     * 输出：所有对外 JSON 字段名都与 Jackson snake_case 运行时契约一致。
     */
    @Test
    @DisplayName("customizer normalizes component schemas to snake case")
    void customizer_componentSchemas_normalizesPropertiesAndRequiredNamesToSnakeCase() {
        contextRunner.run(context -> {
            OpenApiCustomizer customizer = context.getBean(OpenApiCustomizer.class);
            ObjectSchema nested = new ObjectSchema();
            nested.addProperty("eventId", new StringSchema());
            nested.setRequired(List.of("eventId"));
            ObjectSchema root = new ObjectSchema();
            root.addProperty("accessToken", new StringSchema());
            root.addProperty("HTTPServerId", new StringSchema());
            root.addProperty("eventItems", new ArraySchema().items(nested));
            root.setRequired(List.of("accessToken", "eventItems"));
            OpenAPI openAPI = new OpenAPI().components(new Components().addSchemas("RealtimePayload", root));

            customizer.customise(openAPI);

            assertThat(root.getProperties()).containsOnlyKeys("access_token", "http_server_id", "event_items");
            assertThat(root.getRequired()).containsExactly("access_token", "event_items");
            assertThat(nested.getProperties()).containsOnlyKeys("event_id");
            assertThat(nested.getRequired()).containsExactly("event_id");
        });
    }

    /**
     * 验证所有返回 201 的创建类控制器方法都显式声明 OpenAPI Created 响应。
     * 输入：创建频道、发送消息和转发消息的控制器方法。
     * 输出：三个方法的 OpenAPI 响应契约均包含 201。
     */
    @Test
    @DisplayName("created controller operations declare 201 response")
    void controllers_createdOperations_declareCreatedResponse() {
        assertThat(responseCodes(ChannelLifecycleController.class, "createChannel")).contains("201");
        assertThat(responseCodes(ChannelMessageController.class, "sendChannelMessage")).contains("201");
        assertThat(responseCodes(MessageController.class, "forwardMessage")).contains("201");
    }

    /**
     * 验证所有运行时返回 204 的控制器方法都显式声明 No Content 响应。
     * 输入：频道、文件、置顶、通知和用户资料写接口。
     * 输出：每个方法的 OpenAPI 注解均包含 204，不再由 springdoc 错推断为 200。
     */
    @Test
    @DisplayName("no content controller operations declare 204 response")
    void controllers_noContentOperations_declare204Response() {
        assertThat(responseCodes(ChannelLifecycleController.class, "deleteChannel")).contains("204");
        assertThat(responseCodes(ChannelLifecycleController.class, "updateChannelProfile")).contains("204");
        assertThat(responseCodes(ChannelMemberGovernanceController.class, "promoteChannelMemberV1")).contains("204");
        assertThat(responseCodes(ChannelMemberGovernanceController.class, "demoteChannelAdminV1")).contains("204");
        assertThat(responseCodes(ChannelNotificationPreferenceController.class, "updateChannelNotificationPreference")).contains("204");
        assertThat(responseCodes(FileController.class, "uploadFile")).contains("204");
        assertThat(responseCodes(ChannelPinsController.class, "unpinChannelMessage")).contains("204");
        assertThat(responseCodes(NotificationPreferenceController.class, "updateServerNotificationPreference")).contains("204");
        assertThat(responseCodes(UserProfileController.class, "patchCurrentUserProfile")).contains("204");
        assertThat(responseCodes(CurrentUserAccountController.class, "updateEmail")).contains("204");
    }

    /**
     * 验证文件下载同时声明直接二进制响应和重定向响应。
     */
    @Test
    @DisplayName("file download declares direct and redirect responses")
    void fileDownload_declares200And302Responses() {
        assertThat(responseCodes(FileController.class, "download")).contains("200", "302");
    }

    /**
     * 验证 OpenAPI 路径模板及非 header 参数会统一转换为 snake_case。
     * 输入：包含 camelCase path、path/query 参数和标准 header 参数的操作。
     * 输出：path/query 转换完成，header 名保持原样。
     */
    @Test
    @DisplayName("customizer normalizes path and operation parameters to snake case")
    void customizer_pathAndParameters_normalizesToSnakeCase() {
        contextRunner.run(context -> {
            OpenApiCustomizer customizer = context.getBean(OpenApiCustomizer.class);
            Operation operation = new Operation().parameters(new java.util.ArrayList<>(List.of(
                    new Parameter().name("channelId").in("path"),
                    new Parameter().name("beforeMid").in("query"),
                    new Parameter().name("Idempotency-Key").in("header")
            )));
            Parameter componentParameter = new Parameter().name("targetAccountId").in("path");
            OpenAPI openAPI = new OpenAPI()
                    .components(new Components().addParameters("TargetAccountId", componentParameter))
                    .paths(new Paths()
                            .addPathItem("/api/channels/{channelId}/messages", new PathItem().get(operation)));

            customizer.customise(openAPI);

            assertThat(openAPI.getPaths()).containsKey("/api/channels/{channel_id}/messages");
            assertThat(openAPI.getPaths()).doesNotContainKey("/api/channels/{channelId}/messages");
            assertThat(operation.getParameters()).extracting(Parameter::getName)
                    .containsExactly("channel_id", "before_mid", "Idempotency-Key");
            assertThat(componentParameter.getName()).isEqualTo("target_account_id");
        });
    }

    /**
     * 验证真实可能返回冲突的频道删除和申请审批路由声明 409。
     */
    @Test
    @DisplayName("customizer declares conflict responses on channel state routes")
    void customizer_channelConflictRoutes_declares409Responses() {
        contextRunner.run(context -> {
            OpenApiCustomizer customizer = context.getBean(OpenApiCustomizer.class);
            Operation deleteChannelOperation = new Operation();
            Operation decideApplicationOperation = new Operation();
            OpenAPI openAPI = new OpenAPI().paths(new Paths()
                    .addPathItem("/api/channels/{channelId}", new PathItem().delete(deleteChannelOperation))
                    .addPathItem("/api/channels/{channelId}/applications/{applicationId}/decisions",
                            new PathItem().post(decideApplicationOperation)));

            customizer.customise(openAPI);

            assertThat(deleteChannelOperation.getResponses()).containsKey("409");
            assertThat(decideApplicationOperation.getResponses()).containsKey("409");
        });
    }

    /**
     * 验证已有控制器错误响应会补齐 Bearer header 和统一 JSON 内容，并按方法声明协议错误码。
     */
    @Test
    @DisplayName("customizer repairs existing errors and protocol responses")
    void customizer_existingErrorsAndProtocolResponses_areNormalized() {
        contextRunner.run(context -> {
            OpenApiCustomizer customizer = context.getBean(OpenApiCustomizer.class);
        Operation existingUnauthorized = new Operation().responses(new io.swagger.v3.oas.models.responses.ApiResponses()
                    .addApiResponse("401", new ApiResponse().description("未认证")));
            Operation listChannel = new Operation();
            Operation deleteChannel = new Operation();
            Operation uploadAttachment = new Operation().requestBody(new RequestBody());
            OpenAPI openAPI = new OpenAPI().paths(new Paths()
                    .addPathItem("/api/users/me", new PathItem().get(existingUnauthorized))
                    .addPathItem("/api/channels/{channelId}", new PathItem()
                            .get(listChannel)
                            .delete(deleteChannel))
                    .addPathItem("/api/channels/{channelId}/messages/attachments",
                            new PathItem().post(uploadAttachment)));

            customizer.customise(openAPI);

            assertThat(existingUnauthorized.getResponses().get("401").getHeaders())
                    .containsKey("WWW-Authenticate");
            assertThat(existingUnauthorized.getResponses().get("401").getContent())
                    .containsKey("application/json");
            assertThat(listChannel.getResponses()).doesNotContainKey("409");
            assertThat(deleteChannel.getResponses()).containsKey("409");
            assertThat(uploadAttachment.getResponses()).containsKeys("406", "413", "415");
        });
    }

    /**
     * 验证受保护操作的 OpenAPI 401 响应声明 Bearer challenge header。
     */
    @Test
    @DisplayName("customizer declares bearer challenge on unauthorized response")
    void customizer_protectedOperation_declaresBearerChallengeHeader() {
        contextRunner.run(context -> {
            OpenApiCustomizer customizer = context.getBean(OpenApiCustomizer.class);
            Operation operation = new Operation();
            Operation loginOperation = new Operation();
            OpenAPI openAPI = new OpenAPI().paths(new Paths()
                    .addPathItem("/api/users/me", new PathItem().get(operation))
                    .addPathItem("/api/auth/login", new PathItem().post(loginOperation)));

            customizer.customise(openAPI);

            assertThat(operation.getResponses().get("401").getHeaders())
                    .containsKey("WWW-Authenticate");
            assertThat(loginOperation.getResponses()).containsKeys("401", "403");
            assertThat(loginOperation.getResponses().get("401").getHeaders())
                    .containsKey("WWW-Authenticate");
        });
    }

    /**
     * 验证 OpenAPI 自定义器会为关键写接口补充可被 Apifox 导入的请求体示例。
     * 输入：登录、发送消息和历史消息查询路径。
     * 输出：仅 POST 写接口获得 JSON 示例，GET 查询接口不会被错误写入请求体。
     */
    @Test
    @DisplayName("customizer adds request examples for key write operations")
    void customizer_keyWriteOperations_addsRequestExamples() {
        contextRunner.run(context -> {
            OpenApiCustomizer customizer = context.getBean(OpenApiCustomizer.class);

            Operation loginOperation = new Operation();
            Operation updateUserOperation = new Operation();
            Operation uploadBackgroundOperation = new Operation();
            Operation createFileUploadOperation = new Operation();
            Operation uploadFileOperation = new Operation();
            Operation banMemberOperation = new Operation();
            Operation createApplicationOperation = new Operation();
            Operation decideApplicationOperation = new Operation();
            Operation sendMessageOperation = new Operation();
            Operation uploadAttachmentOperation = new Operation();
            Operation pinMessageOperation = new Operation();
            Operation forwardMessageOperation = new Operation();
            Operation readStateOperation = new Operation();
            Operation markMentionsReadOperation = new Operation();
            Operation listMessageOperation = new Operation();

            OpenAPI openAPI = new OpenAPI().paths(new Paths()
                    .addPathItem("/api/auth/login", new PathItem().post(loginOperation))
                    .addPathItem("/api/users/me", new PathItem().patch(updateUserOperation))
                    .addPathItem("/api/users/me/background", new PathItem().post(uploadBackgroundOperation))
                    .addPathItem("/api/files/uploads", new PathItem().post(createFileUploadOperation))
                    .addPathItem("/api/files/uploads/{shareKey}", new PathItem().put(uploadFileOperation))
                    .addPathItem("/api/channels/{channelId}/bans/{targetAccountId}", new PathItem().put(banMemberOperation))
                    .addPathItem("/api/channels/{channelId}/applications", new PathItem().post(createApplicationOperation))
                    .addPathItem("/api/channels/{channelId}/applications/{applicationId}/decisions", new PathItem().post(decideApplicationOperation))
                    .addPathItem("/api/channels/{channelId}/messages", new PathItem()
                            .post(sendMessageOperation)
                            .get(listMessageOperation))
                    .addPathItem("/api/channels/{channelId}/messages/attachments", new PathItem().post(uploadAttachmentOperation))
                    .addPathItem("/api/channels/{channelId}/pins/{messageId}", new PathItem().post(pinMessageOperation))
                    .addPathItem("/api/messages/{messageId}/forward", new PathItem().post(forwardMessageOperation))
                    .addPathItem("/api/channels/{channelId}/read_state", new PathItem().put(readStateOperation))
                    .addPathItem("/api/mentions/read_state", new PathItem().put(markMentionsReadOperation))
            );

            customizer.customise(openAPI);

            assertThat(jsonExamples(loginOperation))
                    .containsKey("测试账号登录");
            assertThat(jsonExamples(loginOperation).get("测试账号登录").getValue().toString())
                    .contains("\"username\": \"carry-owner\"");
            assertThat(jsonExamples(updateUserOperation))
                    .containsKey("更新当前用户资料");
            assertThat(jsonExamples(updateUserOperation).get("更新当前用户资料").getValue().toString())
                    .contains("\"sex\": 0")
                    .contains("\"birthday\": 0");
            assertThat(jsonExamples(createFileUploadOperation))
                    .containsKey("申请文件上传");
            assertThat(binaryExamples(uploadFileOperation))
                    .containsKey("上传文件内容");
            assertThat(jsonExamples(banMemberOperation))
                    .containsKey("禁言频道成员");
            assertThat(jsonExamples(createApplicationOperation))
                    .containsKey("申请加入频道");
            assertThat(jsonExamples(decideApplicationOperation))
                    .containsKey("审批入群申请");

            assertThat(jsonExamples(sendMessageOperation))
                    .containsKey("发送文本消息");
            assertThat(jsonExamples(sendMessageOperation).get("发送文本消息").getValue().toString())
                    .contains("\"domain_version\": \"1.0.0\"")
                    .contains("\"text\": \"hello from Apifox\"")
                    .contains("\"mentions\": [\"{{accountId}}\"]");
            assertThat(formExamples(uploadBackgroundOperation))
                    .containsKey("上传用户背景图");
            assertThat(formExamples(uploadAttachmentOperation))
                    .containsKey("上传消息附件");
            assertThat(jsonExamples(pinMessageOperation))
                    .containsKey("置顶频道消息");
            assertThat(jsonExamples(forwardMessageOperation).get("合并转发消息").getValue().toString())
                    .contains("\"target_cid\": \"{{channelId}}\"")
                    .contains("\"merged_mids\": [\"{{messageId}}\", \"{{secondMessageId}}\"]");
            assertThat(jsonExamples(readStateOperation).get("更新频道已读状态").getValue().toString())
                    .contains("\"last_read_mid\": \"{{messageId}}\"");
            assertThat(jsonExamples(markMentionsReadOperation))
                    .containsKey("批量标记提及已读");
            assertThat(jsonExamples(markMentionsReadOperation).get("批量标记提及已读").getValue().toString())
                    .contains("\"before_mention_id\": \"{{mentionId}}\"")
                    .contains("\"cid\": \"{{channelId}}\"");

            assertThat(listMessageOperation.getRequestBody()).isNull();
        });
    }

    /**
     * 验证依赖邮件或对象存储的路由会声明稳定 503 契约。
     * 输入：邮件验证码、背景图、附件、文件写入和文件下载操作。
     * 输出：所有外部服务边界都包含 503，普通路由不会被错误扩大。
     */
    @Test
    @DisplayName("customizer declares service unavailable responses on external service routes")
    void customizer_externalServiceRoutes_declaresServiceUnavailableResponses() {
        contextRunner.run(context -> {
            OpenApiCustomizer customizer = context.getBean(OpenApiCustomizer.class);
            Operation emailCodeOperation = new Operation();
            Operation backgroundOperation = new Operation();
            Operation attachmentOperation = new Operation();
            Operation fileUploadOperation = new Operation();
            Operation fileDownloadOperation = new Operation();
            Operation ordinaryOperation = new Operation();
            OpenAPI openAPI = new OpenAPI().paths(new Paths()
                    .addPathItem("/api/auth/email_codes", new PathItem().post(emailCodeOperation))
                    .addPathItem("/api/users/me/background", new PathItem().post(backgroundOperation))
                    .addPathItem("/api/channels/{channelId}/messages/attachments", new PathItem().post(attachmentOperation))
                    .addPathItem("/api/files/uploads/{shareKey}", new PathItem().put(fileUploadOperation))
                    .addPathItem("/api/files/download/{shareKey}", new PathItem().get(fileDownloadOperation))
                    .addPathItem("/api/users/me", new PathItem().get(ordinaryOperation))
            );

            customizer.customise(openAPI);

            assertThat(emailCodeOperation.getResponses()).containsKey("503");
            assertThat(backgroundOperation.getResponses()).containsKey("503");
            assertThat(attachmentOperation.getResponses()).containsKey("503");
            assertThat(fileUploadOperation.getResponses()).containsKey("503");
            assertThat(fileDownloadOperation.getResponses()).containsKey("503");
            assertThat(ordinaryOperation.getResponses()).doesNotContainKey("503");
            assertThat(emailCodeOperation.getResponses().get("503").getContent().get("application/json")
                    .getExamples().get("error").getValue().toString()).contains("mail_service_unavailable");
            assertThat(emailCodeOperation.getResponses().get("503").getContent().get("application/json")
                    .getExamples().get("delivery_failed").getValue().toString()).contains("email_delivery_failed");
            assertThat(fileUploadOperation.getResponses().get("503").getContent().get("application/json")
                    .getExamples().get("error").getValue().toString()).contains("storage_service_unavailable");
        });
    }

    /**
     * 验证容易回退为默认 controller 分组名的入口类声明了稳定 OpenAPI tag。
     * 输入：按资源维度拆分但曾缺少类级 `@Tag` 的 Controller。
     * 输出：每个 Controller 都有中文业务分组名，Apifox 导入时不依赖默认类名分组。
     */
    @Test
    @DisplayName("controllers declare stable openapi tags")
    void controllers_openApiGrouping_declaresStableTags() {
        assertThat(tagName(AuditLogController.class)).isEqualTo("审计日志");
        assertThat(tagName(ChannelPinsController.class)).isEqualTo("频道置顶");
        assertThat(tagName(MessageController.class)).isEqualTo("消息资源");
        assertThat(tagName(NotificationPreferenceController.class)).isEqualTo("通知偏好");
    }

    private Map<String, io.swagger.v3.oas.models.examples.Example> jsonExamples(Operation operation) {
        return operation.getRequestBody().getContent().get("application/json").getExamples();
    }

    private Map<String, io.swagger.v3.oas.models.examples.Example> formExamples(Operation operation) {
        return operation.getRequestBody().getContent().get("multipart/form-data").getExamples();
    }

    private Map<String, io.swagger.v3.oas.models.examples.Example> binaryExamples(Operation operation) {
        return operation.getRequestBody().getContent().get("application/octet-stream").getExamples();
    }

    private String tagName(Class<?> controllerType) {
        return controllerType.getAnnotation(io.swagger.v3.oas.annotations.tags.Tag.class).name();
    }

    private List<String> responseCodes(Class<?> controllerType, String methodName) {
        Method method = java.util.Arrays.stream(controllerType.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(methodName))
                .findFirst()
                .orElseThrow();
        return java.util.Arrays.stream(method.getAnnotation(ApiResponses.class).value())
                .map(io.swagger.v3.oas.annotations.responses.ApiResponse::responseCode)
                .toList();
    }
}
