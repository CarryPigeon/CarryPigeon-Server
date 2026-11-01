package team.carrypigeon.backend.chat.domain.config.http.security;

import java.io.IOException;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import team.carrypigeon.backend.chat.domain.features.auth.domain.api.token.AuthTokenApi;
import team.carrypigeon.backend.chat.domain.features.auth.domain.api.token.result.AccessTokenAuthenticationResult;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.logging.LogContexts;

/**
 * JWT接入认证系统的具体filter
 * 职责：进行权限校验
 * 边界：仅用于访问用户权限
 * */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final AuthTokenApi authTokenApi;

    public JwtAuthenticationFilter(AuthTokenApi authTokenApi) {
        this.authTokenApi = authTokenApi;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        // 获取请求头中的认证信息
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        // 判断是否有认证信息
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        // 获取认证信息
        String token = authHeader.substring(7);
        AccessTokenAuthenticationResult result;
        try {
             result = authTokenApi.authenticate(token);
        }catch (Exception e) {
            SecurityContextHolder.clearContext();
            if (e instanceof ProblemException problemException) {
                log.info("HTTP authentication rejected, method={}, reason={}",
                        request.getMethod(), problemException.reason());
            } else {
                log.warn("HTTP authentication failed unexpectedly, method={}",
                        request.getMethod(), e);
            }
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"code\":401,\"message\":\"authentication is required\"}");
            return;
        }
        // TODO 用户权限分流
        LogContexts.uid(String.valueOf(result.accountId()));
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        var principal = new CpPrincipal(result.accountId());
        var authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                authorities
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }
}
