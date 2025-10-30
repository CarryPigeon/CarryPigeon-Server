package team.carrypigeon.backend.starter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import team.carrypigeon.backend.api.starter.connection.ConnectionConfig;
import team.carrypigeon.backend.api.starter.connection.ConnectionStarter;

/**
 * 服务端启动main函数，整个服务端的启动入口，不应该包含任何与业务逻辑相关的代码
 * @author midreamsheep
 * */
// 声明springboot应用
@SpringBootApplication
// 允许定时任务
@EnableScheduling
// 扫描team.carrypigeon.backend包下的所有类文件
// 插件相关包命名规范为team.carrypigeon.backend.plugin.{{PluginName}}
@ComponentScan(basePackages = {"team.carrypigeon.backend"})
// 启动日志打印
@Slf4j
public class ApplicationStarter {
    public static void main(String[] args) {
        // 输出日志，通知项目启动
        log.info("Application is starting...");
        // 启动容器注入服务并获取对应上下文
        new SpringApplication(ApplicationStarter.class).run(args);
        // 输出日志，通知springboot初始化完成
        log.info("spring context ready.");
    }

}
