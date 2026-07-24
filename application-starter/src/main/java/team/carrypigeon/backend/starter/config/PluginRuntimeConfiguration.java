package team.carrypigeon.backend.starter.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.api.PluginRuntimeApi;

/**
 * 插件运行时启动装配。
 *
 * <p>职责：把 plugin feature 的运行时 API 接入 Spring Boot 启动生命周期，并注册全局就绪门禁。
 * 边界：不实现插件业务，不改变插件的 classpath 发现方式。</p>
 */
@Configuration
public class PluginRuntimeConfiguration {

    @Bean
    public PluginReadinessGate pluginReadinessGate() {
        return new PluginReadinessGate();
    }

    @Bean
    public PluginStartupRunner pluginStartupRunner(
            PluginRuntimeApi pluginRuntimeApi,
            PluginReadinessGate readinessGate
    ) {
        return new PluginStartupRunner(pluginRuntimeApi, readinessGate);
    }

    @Bean
    public FilterRegistrationBean<PluginStartupGateFilter> pluginStartupGateFilter(
            PluginReadinessGate readinessGate
    ) {
        FilterRegistrationBean<PluginStartupGateFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new PluginStartupGateFilter(readinessGate));
        registration.addUrlPatterns("/*");
        registration.setOrder(Integer.MIN_VALUE);
        registration.setName("pluginStartupGateFilter");
        return registration;
    }
}
