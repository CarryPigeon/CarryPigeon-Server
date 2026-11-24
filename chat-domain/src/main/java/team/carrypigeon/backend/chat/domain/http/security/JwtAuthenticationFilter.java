package team.carrypigeon.backend.chat.domain.http.security;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

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

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthTokenApi authTokenApi;
    private final SecurityErrorResponses errors;

    public JwtAuthenticationFilter(AuthTokenApi authTokenApi, SecurityErrorResponses errors) {
        this.authTokenApi = authTokenApi;
        this.errors = errors;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 探针自行校验来源，仅返回就绪状态。
        return (request.getContextPath() + "/internal/readiness").equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        // 获取请求头中的认证信息
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        // 判断是否有认证信息；Bearer scheme 按 RFC 大小写不敏感
        if (authHeader == null
                || authHeader.length() < BEARER_PREFIX.length()
                || !authHeader.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            filterChain.doFilter(request, response);
            return;
        }
        // 获取认证信息
        String token = authHeader.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }
        AccessTokenAuthenticationResult result;
        try {
             result = authTokenApi.authenticate(token);
        } catch (ProblemException exception) {
            SecurityContextHolder.clearContext();
            if (exception.status() >= 500) {
                log.error("HTTP authentication failed, method={}, reason={}",
                        request.getMethod(), exception.reason(), exception);
            } else {
                log.info("HTTP authentication rejected, method={}, reason={}",
                        request.getMethod(), exception.reason());
            }
            errors.write(request, response, exception);
            return;
        } catch (RuntimeException exception) {
            SecurityContextHolder.clearContext();
            log.error("HTTP authentication failed unexpectedly, method={}", request.getMethod(), exception);
            errors.write(request, response, exception);
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
