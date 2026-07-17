package team.carrypigeon.backend.chat.domain.features.auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import team.carrypigeon.backend.chat.domain.features.auth.controller.support.AuthAccessTokenInterceptor;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.port.AuthTokenService;
import team.carrypigeon.backend.chat.domain.shared.controller.support.RequestAuthenticationContext;

/**
 * 鉴权 HTTP 拦截配置。
 * 职责：将 access token 校验接入 Spring MVC 受保护 API。
 * 边界：只区分匿名与已认证，不实现角色权限。
 */
@Configuration
public class AuthWebMvcConfiguration implements WebMvcConfigurer {

    private final AuthTokenService authTokenService;
    private final RequestAuthenticationContext authRequestContext;

    public AuthWebMvcConfiguration(AuthTokenService authTokenService, RequestAuthenticationContext authRequestContext) {
        this.authTokenService = authTokenService;
        this.authRequestContext = authRequestContext;
    }

    /**
     * 注册 access token 校验拦截器。
     * 输入：Spring MVC 拦截器注册表。
     * 副作用：为 `/api/**` 受保护接口挂载鉴权拦截器，并显式放行匿名入口。
     *
     * @param registry Spring MVC 拦截器注册表
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AuthAccessTokenInterceptor(authTokenService, authRequestContext))
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/server",
                        "/api/gates/required/check",
                        "/api/plugins/catalog",
                        "/api/domains/catalog",
                        "/api/files/download/server_avatar",
                        "/api/auth/register",
                        "/api/auth/login",
                        "/api/auth/email_codes",
                        "/api/auth/tokens",
                        "/api/auth/refresh",
                        "/api/auth/revoke"
                );
    }
}
