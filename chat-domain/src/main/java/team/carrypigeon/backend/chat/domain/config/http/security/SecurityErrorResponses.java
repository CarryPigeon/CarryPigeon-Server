package team.carrypigeon.backend.chat.domain.config.http.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.util.ServletRequestPathUtils;

/** 认证在 DispatcherServlet 之前失败时，按实际路由选择与 MVC 相同的错误处理器。 */
@Component
final class SecurityErrorResponses {

    private final HandlerMapping routes;
    private final HandlerExceptionResolver exceptions;

    SecurityErrorResponses(
        @Lazy @Qualifier("requestMappingHandlerMapping") HandlerMapping routes,
        @Lazy @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptions
    ) {
        this.routes = routes;
        this.exceptions = exceptions;
    }

    void write(HttpServletRequest request, HttpServletResponse response, Exception failure) {
        if (response.isCommitted()) {
            return;
        }
        Object handler = null;
        String path = ServletRequestPathUtils.parseAndCache(request).pathWithinApplication().value();
        // 只解析映射，不调用控制器。未知路径/错误方法优先返回 404/405，避免被认证错误遮蔽。
        // Swagger 静态资源由独立安全策略保护，不属于业务路由。
        if (!path.startsWith("/swagger-ui") && !path.startsWith("/v3/api-docs")) {
            try {
                HandlerExecutionChain route = routes.getHandler(request);
                if (route == null) {
                    failure = new NoHandlerFoundException(request.getMethod(), path, new HttpHeaders());
                } else {
                    handler = route.getHandler();
                }
            } catch (Exception routingFailure) {
                failure = routingFailure;
            }
        }
        if (exceptions.resolveException(request, response, handler, failure) == null) {
            throw new IllegalStateException("Cannot render HTTP error", failure);
        }
    }
}
