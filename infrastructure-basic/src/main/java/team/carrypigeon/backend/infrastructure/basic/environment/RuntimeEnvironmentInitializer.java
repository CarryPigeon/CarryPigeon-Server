package team.carrypigeon.backend.infrastructure.basic.environment;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/** 在组件扫描前检查环境选择，避免两种投递或持久化实现同时生效。 */
public class RuntimeEnvironmentInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext context) {
        RuntimeEnvironment.selected(context.getEnvironment());
    }
}
