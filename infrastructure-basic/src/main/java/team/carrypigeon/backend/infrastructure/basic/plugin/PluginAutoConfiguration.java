package team.carrypigeon.backend.infrastructure.basic.plugin;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 插件配置基础设施自动配置。
 * 职责：注册统一插件配置绑定与查询入口。
 * 边界：这里只装配插件配置能力，不装配具体插件实现。
 */
@AutoConfiguration
@EnableConfigurationProperties(PluginProperties.class)
public class PluginAutoConfiguration {

    /**
     * 创建插件配置查询入口。
     *
     * @param properties 统一插件配置属性
     * @return 插件配置查询 provider
     */
    @Bean
    public PluginConfigurationProvider pluginConfigurationProvider(PluginProperties properties) {
        return new PluginConfigurationProvider(properties);
    }
}
