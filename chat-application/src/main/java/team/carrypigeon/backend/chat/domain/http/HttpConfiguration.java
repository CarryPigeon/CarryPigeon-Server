package team.carrypigeon.backend.chat.domain.http;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import team.carrypigeon.backend.chat.domain.http.cors.HttpCorsProperties;
import team.carrypigeon.backend.chat.domain.http.log.HttpRequestMdcFilter;
import team.carrypigeon.backend.chat.domain.http.log.HttpRequestLoggingFilter;

/**
 * HTTP装配配置。
 */
@Configuration
@EnableConfigurationProperties(HttpCorsProperties.class)
public class HttpConfiguration implements WebMvcConfigurer {

    private final HttpCorsProperties properties;

    public HttpConfiguration(HttpCorsProperties properties) {
        this.properties = properties;
    }

    /**
     * 按本地调试配置注册 `/api/**` CORS 规则。
     *
     * @param registry Spring MVC CORS 注册表
     */
    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        HttpCorsProperties.Cors cors = properties.cors();
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

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                HttpRequestMdcFilter.useMatchedRoute(request);
                return true;
            }
        }).addPathPatterns("/api/**");
    }

    /**
     * 注册本地请求摘要日志过滤器。
     *
     * @return 过滤器注册 Bean
     */
    @Bean
    @ConditionalOnProperty(name = "cp.local-dev.http.request-log.enabled", havingValue = "true")
    public FilterRegistrationBean<HttpRequestLoggingFilter> httpRequestDebugLoggingFilterRegistration() {
        FilterRegistrationBean<HttpRequestLoggingFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new HttpRequestLoggingFilter());
        registration.addUrlPatterns("/api/*");
        registration.setName("httpRequestDebugLoggingFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 2);
        return registration;
    }

    /**
     * 注册MDC过滤器
     *
     * @return 过滤器注册 Bean
     * */
    @Bean
    public FilterRegistrationBean<HttpRequestMdcFilter> httpRequestMdcFilterRegistration() {
        FilterRegistrationBean<HttpRequestMdcFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new HttpRequestMdcFilter());
        registration.addUrlPatterns("/api/*");
        registration.setName("httpRequestMdcFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE+1);
        return registration;
    }
}
