package team.carrypigeon.backend.starter.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 本地 HTTP 调试装配配置。
 * 职责：按显式配置为本地客户端联调注册 CORS 与请求摘要日志。
 * 边界：该配置仅做 Spring Boot 启动层装配，不改变业务接口协议。
 */
@Configuration
@EnableConfigurationProperties(LocalHttpDebugProperties.class)
public class LocalHttpDebugConfiguration implements WebMvcConfigurer {

    private final LocalHttpDebugProperties properties;

    public LocalHttpDebugConfiguration(LocalHttpDebugProperties properties) {
        this.properties = properties;
    }

    /**
     * 按本地调试配置注册 `/api/**` CORS 规则。
     *
     * @param registry Spring MVC CORS 注册表
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        LocalHttpDebugProperties.Cors cors = properties.cors();
        if (!cors.enabled()) {
            return;
        }
        registry.addMapping("/api/**")
                .allowedOriginPatterns(cors.allowedOriginPatterns().toArray(String[]::new))
                .allowedMethods(cors.allowedMethods().toArray(String[]::new))
                .allowedHeaders(cors.allowedHeaders().toArray(String[]::new))
                .exposedHeaders(cors.exposedHeaders().toArray(String[]::new))
                .allowCredentials(false)
                .maxAge(cors.maxAge());
    }

    /**
     * 注册本地请求摘要日志过滤器。
     *
     * @return 过滤器注册 Bean
     */
    @Bean
    @ConditionalOnProperty(prefix = "cp.local-dev.http.request-log", name = "enabled", havingValue = "true")
    public FilterRegistrationBean<LocalHttpRequestDebugLoggingFilter> localHttpRequestDebugLoggingFilterRegistration() {
        FilterRegistrationBean<LocalHttpRequestDebugLoggingFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new LocalHttpRequestDebugLoggingFilter());
        registration.addUrlPatterns("/api/*");
        registration.setName("localHttpRequestDebugLoggingFilter");
        registration.setOrder(Ordered.LOWEST_PRECEDENCE);
        return registration;
    }
}
