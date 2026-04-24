package team.carrypigeon.backend.starter.config;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import team.carrypigeon.backend.infrastructure.basic.startup.InitializationCheck;
import team.carrypigeon.backend.infrastructure.basic.startup.InitializationCheckFailureException;
import team.carrypigeon.backend.infrastructure.basic.startup.InitializationCheckResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * 初始化检查装配测试。
 * 职责：验证 starter 是否只负责编排并执行共享初始化检查契约。
 * 边界：不接入真实外部服务，只验证执行器装配与失败语义。
 */
@Tag("smoke")
class InitializationCheckConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(InitializationCheckConfiguration.class));

    /**
     * 验证无检查项时仍会装配执行器。
     * 输入：仅加载初始化检查配置。
     * 输出：上下文存在初始化检查执行器 Bean。
     */
    @Test
    @DisplayName("configuration without checks still registers runner")
    void configuration_withoutChecks_stillRegistersRunner() {
        contextRunner.run(context -> assertThat(context).hasSingleBean(InitializationCheckRunner.class));
    }

    /**
     * 验证全部通过时执行器不会抛出异常。
     * 输入：两个通过的初始化检查。
     * 输出：执行器顺利执行完成。
     */
    @Test
    @DisplayName("runner with passing checks completes successfully")
    void runner_withPassingChecks_completesSuccessfully() {
        contextRunner
                .withUserConfiguration(PassingChecksConfiguration.class)
                .run(context -> {
                    InitializationCheckRunner runner = context.getBean(InitializationCheckRunner.class);
                    assertDoesNotThrow(runner::afterSingletonsInstantiated);
                });
    }

    /**
     * 验证必需检查失败时会阻止启动。
     * 输入：包含一个失败的必需检查。
     * 输出：容器在启动阶段因初始化检查失败而中断。
     */
    @Test
    @DisplayName("runner with failing required check throws exception")
    void runner_withFailingRequiredCheck_throwsException() {
        contextRunner
                .withUserConfiguration(FailingRequiredCheckConfiguration.class)
                .run(context -> assertThat(context.getStartupFailure())
                        .isInstanceOf(InitializationCheckFailureException.class)
                        .hasMessage("Initialization check failed [storage]: storage bucket check failed"));
    }

    /**
     * 验证必需检查失败时会早于 SmartLifecycle 启动阶段中断。
     * 输入：失败的必需检查与一个可自动启动的 SmartLifecycle Bean。
     * 输出：生命周期 Bean 不会进入启动状态。
     */
    @Test
    @DisplayName("failing required check prevents smart lifecycle startup")
    void failingRequiredCheck_preventsSmartLifecycleStartup() {
        contextRunner
                .withUserConfiguration(FailingRequiredCheckAndLifecycleConfiguration.class)
                .run(context -> {
                    assertThat(context.getStartupFailure())
                            .isInstanceOf(InitializationCheckFailureException.class);
                    assertFalse(FailingRequiredCheckAndLifecycleConfiguration.LIFECYCLE.running);
                });
    }

    @TestConfiguration
    static class PassingChecksConfiguration {

        @Bean
        InitializationCheck databaseInitializationCheck() {
            return namedPassingCheck("database", "database ready");
        }

        @Bean
        InitializationCheck cacheInitializationCheck() {
            return namedPassingCheck("cache", "cache ready");
        }
    }

    @TestConfiguration
    static class FailingRequiredCheckConfiguration {

        @Bean
        InitializationCheck databaseInitializationCheck() {
            return namedPassingCheck("database", "database ready");
        }

        @Bean
        InitializationCheck storageInitializationCheck() {
            return new InitializationCheck() {
                @Override
                public String name() {
                    return "storage";
                }

                @Override
                public InitializationCheckResult check() {
                    return InitializationCheckResult.failed("storage bucket check failed");
                }
            };
        }
    }

    @TestConfiguration
    static class FailingRequiredCheckAndLifecycleConfiguration {

        private static final TestSmartLifecycle LIFECYCLE = new TestSmartLifecycle();

        @Bean
        InitializationCheck databaseInitializationCheck() {
            return new InitializationCheck() {
                @Override
                public String name() {
                    return "database";
                }

                @Override
                public InitializationCheckResult check() {
                    return InitializationCheckResult.failed("database unavailable");
                }
            };
        }

        @Bean
        TestSmartLifecycle testSmartLifecycle() {
            return LIFECYCLE;
        }
    }

    private static InitializationCheck namedPassingCheck(String name, String message) {
        return new InitializationCheck() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public InitializationCheckResult check() {
                return InitializationCheckResult.passed(message);
            }
        };
    }

    static class TestSmartLifecycle implements SmartLifecycle {

        private boolean running;

        @Override
        public void start() {
            running = true;
        }

        @Override
        public void stop() {
            running = false;
        }

        @Override
        public boolean isRunning() {
            return running;
        }
    }
}
