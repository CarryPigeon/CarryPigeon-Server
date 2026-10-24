package team.carrypigeon.backend.infrastructure.basic.plugin.message;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * 消息处理器的标记注解，value标记的是消息领域，格式问`domain:type:version`
 * 职责：标记消息处理器，并提供domain用于消息分配
 * 边界：不关注具体的行为逻辑
 * */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface MessageHandler {
    String value();
}
