package team.carrypigeon.backend.starter.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import team.carrypigeon.backend.chat.domain.features.channel.controller.http.AuditLogController;
import team.carrypigeon.backend.chat.domain.features.message.controller.http.ChannelPinsController;
import team.carrypigeon.backend.chat.domain.features.message.controller.http.MessageController;
import team.carrypigeon.backend.chat.domain.features.server.controller.http.NotificationPreferenceController;

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
                    .containsEntry("websocket_enabled_config", "CP_CHAT_REALTIME_ENABLED=true (default); set false to disable")
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
                    .contains("\"text\": \"hello from Apifox\"");
            assertThat(formExamples(uploadBackgroundOperation))
                    .containsKey("上传用户背景图");
            assertThat(formExamples(uploadAttachmentOperation))
                    .containsKey("上传消息附件");
            assertThat(jsonExamples(pinMessageOperation))
                    .containsKey("置顶频道消息");
            assertThat(jsonExamples(forwardMessageOperation).get("转发消息").getValue().toString())
                    .contains("\"target_cid\": \"{{channelId}}\"");
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
}
