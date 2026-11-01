package team.carrypigeon.backend.chat.domain.config.http.security;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
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
            JsonProvider jsonProvider
    ) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            )
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
