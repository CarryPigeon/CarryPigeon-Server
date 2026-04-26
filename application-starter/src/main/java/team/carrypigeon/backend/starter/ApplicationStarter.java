package team.carrypigeon.backend.starter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring Boot 启动入口。
 * <p>
 * 职责：启动 Spring 容器，并作为最终运行时的装配入口。
 * 边界：该类只负责框架能力开关与组件扫描，不承载核心业务规则。
 * 依赖：业务能力来自 `chat-domain`，固定基础设施能力来自 `infrastructure-basic`，
 * 具体外部实现后续由 `infrastructure-service` 的 `*-impl` 在启动层完成装配。
 */
@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = {"team.carrypigeon.backend"})
@Slf4j
public class ApplicationStarter {
    /**
     * Spring Boot 应用入口。
     *
     * @param args 启动参数，仅用于传递运行环境相关配置，不参与业务语义处理
     */
    public static void main(String[] args) {
        log.info("Application is starting...");
        new SpringApplication(ApplicationStarter.class).run(args);
        log.info("Application is running ...");
    }

}
