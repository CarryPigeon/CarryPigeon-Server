package team.carrypigeon.backend.starter.config;

import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 本地 HTTP 调试配置测试。
 * 职责：验证 starter 层本地 CORS 与请求日志开关的装配契约。
 * 边界：不验证浏览器 CORS 实现，只验证 Spring MVC 注册结果与过滤器行为。
 */
@Tag("contract")
class LocalHttpDebugConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(LocalHttpDebugConfiguration.class);

    /**
     * 验证默认配置不会注册 CORS 规则或请求日志过滤器。
     */
    @Test
    @DisplayName("configuration keeps local debug features disabled by default")
    void configuration_default_keepsLocalDebugFeaturesDisabled() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(LocalHttpDebugProperties.class);
            assertThat(context).doesNotHaveBean(FilterRegistrationBean.class);

            LocalHttpDebugConfiguration configuration = context.getBean(LocalHttpDebugConfiguration.class);
            TestCorsRegistry registry = new TestCorsRegistry();
            configuration.addCorsMappings(registry);

            assertThat(registry.exposedCorsConfigurations()).isEmpty();
        });
    }

    /**
     * 验证启用本地 CORS 后只为 `/api/**` 注册本地来源白名单。
     */
    @Test
    @DisplayName("configuration registers local cors mappings when enabled")
    void configuration_corsEnabled_registersLocalCorsMappings() {
        contextRunner
                .withPropertyValues(
                        "cp.local-dev.http.cors.enabled=true",
                        "cp.local-dev.http.cors.allowed-origin-patterns=http://127.0.0.1:*,http://localhost:*"
                )
                .run(context -> {
                    LocalHttpDebugConfiguration configuration = context.getBean(LocalHttpDebugConfiguration.class);
                    TestCorsRegistry registry = new TestCorsRegistry();
                    configuration.addCorsMappings(registry);

                    Map<String, CorsConfiguration> configurations = registry.exposedCorsConfigurations();
                    assertThat(configurations).containsOnlyKeys("/api/**");
                    assertThat(configurations.get("/api/**").getAllowedOriginPatterns())
                            .containsExactly("http://127.0.0.1:*", "http://localhost:*");
                    assertThat(configurations.get("/api/**").getAllowedMethods())
                            .contains("GET", "POST", "OPTIONS");
                });
    }

    /**
     * 验证请求日志过滤器只在显式开启时装配。
     */
    @Test
    @DisplayName("configuration registers request log filter only when enabled")
    void configuration_requestLogEnabled_registersFilter() {
        contextRunner
                .withPropertyValues("cp.local-dev.http.request-log.enabled=true")
                .run(context -> assertThat(context).hasSingleBean(FilterRegistrationBean.class));
    }

    /**
     * 验证请求摘要日志会脱敏常见敏感查询参数，且不影响请求链路执行。
     */
    @Test
    @DisplayName("request log filter writes sanitized request summary")
    void doFilter_requestWithSensitiveQuery_logsSanitizedSummary() throws ServletException, IOException {
        LocalHttpRequestDebugLoggingFilter filter = new LocalHttpRequestDebugLoggingFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/server");
        request.setQueryString("access_token=secret-token&plain=1");
        request.addHeader("Origin", "http://127.0.0.1:5173");
        request.addHeader("User-Agent", "local-client");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response) {
                ((MockHttpServletResponse) response).setStatus(204);
            }
        });

        assertThat(response.getStatus()).isEqualTo(204);
        assertThat(filter.buildRequestSummary(request, response, 12L, null))
                .contains("Action: local_http_request_completed")
                .contains("method=GET")
                .contains("origin=http://127.0.0.1:5173")
                .contains("access_token=***")
                .doesNotContain("secret-token");
    }

    /**
     * `TestCorsRegistry` 测试辅助类型。
     * 职责：暴露 Spring MVC protected CORS 配置快照，便于验证当前配置注册契约。
     */
    private static final class TestCorsRegistry extends CorsRegistry {

        private Map<String, CorsConfiguration> exposedCorsConfigurations() {
            return getCorsConfigurations();
        }
    }
}
