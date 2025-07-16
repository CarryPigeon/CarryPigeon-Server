package team.carrypigeon.backend.api.chat.domain.controller;

import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于标记处理器的注解，标记的处理器必须实现{@link CPController}接口
 * value：处理器处理的路径
 * */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface CPControllerTag {
    String value();
}
