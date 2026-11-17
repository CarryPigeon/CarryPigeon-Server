package team.carrypigeon.backend.chat.domain.config.http.security;

import jakarta.servlet.DispatcherType;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;

/***
 * spring security配置，用于项目中的鉴权和认证功能
 * 职责：将 access token 校验接入 Spring MVC 受保护 API。
 * 边界：区分匿名与已认证，实现角色认证
 * */
@Configuration
@EnableMethodSecurity(jsr250Enabled = true)
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            JsonProvider jsonProvider,
            Environment environment
    ) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> {})
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> {
                // 错误分派保留原始响应；白名单精确到方法，新增业务入口默认需要认证。
                auth.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                    .requestMatchers(CorsUtils::isPreFlightRequest).permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/server", "/api/files/download/server/head").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login/pwd",
                            "/api/auth/login/email", "/api/auth/email_codes", "/api/auth/refresh", "/api/auth/revoke").permitAll();
                if (environment.acceptsProfiles(Profiles.of("dev"))) {
                    auth.requestMatchers(HttpMethod.GET, "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll();
                } else {
                    // 即使部署误开 springdoc，非开发环境也不通过安全链暴露文档。
                    auth.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").denyAll();
                }
                auth.anyRequest().authenticated();
            })
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new JsonAuthenticationEntryPoint(jsonProvider))
                .accessDeniedHandler(new JsonAccessDeniedHandler(jsonProvider))
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 禁止 Spring Boot 把 JWT filter 另外注册成容器级 servlet filter。
     * 原因：该 filter 只应运行在 Spring Security filter chain 内，避免重复注册和顺序歧义。
     *
     * @param jwtAuthenticationFilter JWT 认证过滤器
     * @return 显式禁用的过滤器注册
     */
    @Bean
    FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilterRegistration(
            JwtAuthenticationFilter jwtAuthenticationFilter
    ) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration =
                new FilterRegistrationBean<>(jwtAuthenticationFilter);
        registration.setEnabled(false);
        return registration;
    }
}
