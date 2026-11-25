package team.carrypigeon.backend.infrastructure.basic.environment;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.lang.NonNull;

/** 组件扫描时读取实现类声明的环境，使用 Spring 当前生效的 profile（含默认环境）匹配。 */
final class EnvironmentComponentCondition implements Condition {
    @Override
    public boolean matches(@NonNull ConditionContext context, AnnotatedTypeMetadata metadata) {
        // 根包扫描也会读取组合注解的定义，只有标注了该注解的实现类才是候选。
        var environment = metadata.getAnnotations().get(InEnvironment.class);
        if (!environment.isPresent()) {
            return false;
        }
        String[] profiles = environment.getStringArray("profiles");
        return profiles.length > 0 && context.getEnvironment().matchesProfiles(profiles);
    }
}
