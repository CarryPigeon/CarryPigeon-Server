package team.carrypigeon.backend.infrastructure.basic.environment;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

/** 标记由组件扫描发现的环境实现。多个环境表示任一匹配，未选择的实现不会注册。 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
@InEnvironment(profiles = {})
public @interface EnvironmentComponent {
    String PROD = "prod";
    String DEV = "dev";
    String TEST = "test";

    /** 允许装配当前组件的运行环境。 */
    @AliasFor(annotation = InEnvironment.class, attribute = "profiles")
    String[] profiles();
}
