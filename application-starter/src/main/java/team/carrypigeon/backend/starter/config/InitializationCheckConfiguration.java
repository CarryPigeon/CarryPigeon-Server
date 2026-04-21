package team.carrypigeon.backend.starter.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import team.carrypigeon.backend.infrastructure.basic.startup.InitializationCheck;

/**
 * 初始化检查装配配置。
 * 职责：在 starter 中装配共享初始化检查执行器。
 * 边界：这里只负责编排执行，不承载具体检查实现。
 */
@Configuration
public class InitializationCheckConfiguration {

    /**
     * 创建初始化检查执行器。
     *
     * @param initializationChecks 当前上下文中的初始化检查集合
     * @return 初始化检查执行器
     */
    @Bean
    public InitializationCheckRunner initializationCheckRunner(List<InitializationCheck> initializationChecks) {
        return new InitializationCheckRunner(initializationChecks);
    }
}
