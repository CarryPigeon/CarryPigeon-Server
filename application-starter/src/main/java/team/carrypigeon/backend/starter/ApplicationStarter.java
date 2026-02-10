package team.carrypigeon.backend.starter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Spring Boot 启动入口。
 * <p>
 * 该类只负责启动容器与基础框架能力开关，不承载业务逻辑。
 * 阅读项目时可从此处作为入口，然后进入 `chat-domain` 的 Controller 与 Flow 链路。
 */
@SpringBootApplication
@EnableScheduling
@EnableTransactionManagement
@ComponentScan(basePackages = {"team.carrypigeon.backend"})
@Slf4j
public class ApplicationStarter {
    /**
     * Spring Boot 应用入口。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        log.info("Application is starting...");
        new SpringApplication(ApplicationStarter.class).run(args);
        log.info("Application is running ...");
    }

}
