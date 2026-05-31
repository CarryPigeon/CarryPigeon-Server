package team.carrypigeon.backend.starter.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

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
            Operation publicTokenOperation = new Operation();
            Operation publicServerOperation = new Operation();
            Operation publicPluginCatalogOperation = new Operation();
            Operation publicDomainCatalogOperation = new Operation();
            Operation publicGateOperation = new Operation();
            Operation protectedChannelOperation = new Operation();

            OpenAPI openAPI = new OpenAPI().paths(new Paths()
                    .addPathItem("/api/users/me", new PathItem().get(protectedOperation))
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
}
