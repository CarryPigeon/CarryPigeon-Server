package team.carrypigeon.backend.api.chat.domain.message;

import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 消息领域处理器标注。
 * <p>
 * 通过 `value` 声明该处理器支持的消息领域，并自动注册为 Spring 组件。
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface CPMessageDomain {

    /**
     * 消息领域标识。
     *
     * @return 消息领域标识字符串。
     */
    @AliasFor(annotation = Component.class)
    String value();
}
