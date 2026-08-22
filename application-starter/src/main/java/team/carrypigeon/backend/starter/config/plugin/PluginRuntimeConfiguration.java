package team.carrypigeon.backend.starter.config.plugin;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.api.PluginRuntimeApi;
import team.carrypigeon.backend.starter.bootstrap.plugin.PluginReadinessGate;
import team.carrypigeon.backend.starter.bootstrap.plugin.PluginStartupLifecycle;
import team.carrypigeon.backend.starter.support.http.PluginStartupGateFilter;

/**
 * 插件运行时启动装配。
 * 职责：把 plugin feature 的运行时 API 接入 Spring 生命周期，并注册全局 HTTP 就绪门禁。
 * 边界：不实现插件业务，不改变插件的 classpath 发现方式。
 */
@Configuration
public class PluginRuntimeConfiguration {

    @Bean
    public PluginReadinessGate pluginReadinessGate() {
        return new PluginReadinessGate();
    }

    @Bean
    public PluginStartupLifecycle pluginStartupLifecycle(
            PluginRuntimeApi pluginRuntimeApi,
            PluginReadinessGate readinessGate
    ) {
        return new PluginStartupLifecycle(pluginRuntimeApi, readinessGate);
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
