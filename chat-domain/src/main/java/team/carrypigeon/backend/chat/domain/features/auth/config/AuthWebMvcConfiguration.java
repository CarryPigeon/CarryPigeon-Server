package team.carrypigeon.backend.chat.domain.features.auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import team.carrypigeon.backend.chat.domain.features.auth.controller.support.AuthAccessTokenInterceptor;
import team.carrypigeon.backend.chat.domain.features.auth.controller.support.AuthRequestContext;
import team.carrypigeon.backend.chat.domain.features.auth.domain.service.AuthTokenService;

/**
 * 鉴权 HTTP 拦截配置。
 * 职责：将 access token 校验接入 Spring MVC 受保护 API。
 * 边界：只区分匿名与已认证，不实现角色权限。
 */
@Configuration
public class AuthWebMvcConfiguration implements WebMvcConfigurer {

    private final AuthTokenService authTokenService;
    private final AuthRequestContext authRequestContext;

    public AuthWebMvcConfiguration(AuthTokenService authTokenService, AuthRequestContext authRequestContext) {
        this.authTokenService = authTokenService;
        this.authRequestContext = authRequestContext;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AuthAccessTokenInterceptor(authTokenService, authRequestContext))
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/register",
                        "/api/auth/login",
                        "/api/auth/refresh",
                        "/api/auth/logout",
                        "/api/server/**"
                );
    }
}
