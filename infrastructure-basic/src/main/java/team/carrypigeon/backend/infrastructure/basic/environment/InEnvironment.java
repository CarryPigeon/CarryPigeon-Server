package team.carrypigeon.backend.infrastructure.basic.environment;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Conditional;

/** 为组件或配置属性声明适用环境，由对应的 Spring 扫描器负责注册。 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(EnvironmentComponentCondition.class)
public @interface InEnvironment {
    String[] profiles();
}
