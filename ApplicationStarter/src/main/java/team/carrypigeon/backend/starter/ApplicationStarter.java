package team.carrypigeon.backend.starter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import team.carrypigeon.backend.api.connection.pool.ConnectionPoolStarter;

/**
 * 服务端启动器
 * */
// 声明springboot应用
@SpringBootApplication
// 扫描team.carrypigeon.backend包下的所有类文件
@ComponentScan(basePackages = {"team.carrypigeon.backend"})
@Slf4j
public class ApplicationStarter {
    public static void main(String[] args) {
        // 启动容器注入服务并获取对应上下文
        ApplicationContext context = new SpringApplication(ApplicationStarter.class).run(args);
        // 启动连接池
        context.getBean(ConnectionPoolStarter.class).run(7609);
    }

}
