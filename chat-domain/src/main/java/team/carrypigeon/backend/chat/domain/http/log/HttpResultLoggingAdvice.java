package team.carrypigeon.backend.chat.domain.http.log;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import team.carrypigeon.backend.chat.domain.shared.result.ErrorResult;
import team.carrypigeon.backend.chat.domain.shared.result.SuccessResult;

/** 在 MVC 选择响应体后提取业务码，流式文件内容直接交由原转换器写出。 */
@ControllerAdvice
@ConditionalOnProperty(name = "cp.local-dev.http.request-log.enabled", havingValue = "true")
public class HttpResultLoggingAdvice implements ResponseBodyAdvice<Object> {
    @Override
    public boolean supports(@NonNull MethodParameter returnType, @NonNull Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, @NonNull MethodParameter returnType, @NonNull MediaType contentType,
                                  @NonNull Class<? extends HttpMessageConverter<?>> converterType, @NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response) {
        if (request instanceof ServletServerHttpRequest servlet) {
            if (body instanceof ErrorResult error) {
                servlet.getServletRequest().setAttribute(HttpRequestLoggingFilter.CODE, error.code());
            } else if (body instanceof SuccessResult<?> success) {
                servlet.getServletRequest().setAttribute(HttpRequestLoggingFilter.CODE, success.code());
            }
        }
        return body;
    }
}
