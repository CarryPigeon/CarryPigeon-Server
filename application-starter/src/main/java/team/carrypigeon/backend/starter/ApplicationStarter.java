package team.carrypigeon.backend.starter;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring Boot 启动入口。
 * <p>
 * 职责：启动 Spring 容器，并作为最终运行时的装配入口。
 */
@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = {"team.carrypigeon.backend"})
@ConfigurationPropertiesScan(basePackages = {"team.carrypigeon.backend"})
@MapperScan(
        basePackages = "team.carrypigeon.backend.infrastructure.service.database.impl.mybatis",
        annotationClass = org.apache.ibatis.annotations.Mapper.class
)
@EnableCaching
@Slf4j
public class ApplicationStarter {
    /**
     * Spring Boot 应用入口。
     *
     * @param args 启动参数，仅用于传递运行环境相关配置，不参与业务语义处理
     */
    public static void main(String[] args) {
        log.info("Application is starting...");
        SpringApplication application = new SpringApplication(ApplicationStarter.class);
        application.addInitializers(ConfigurableApplicationContext::getBeanFactory);
        application.run(args);
        log.info("Application is running ...");
    }

}
