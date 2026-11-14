package team.carrypigeon.backend.infrastructure.service.mail.impl.config;

import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@EnableAsync
@Configuration
public class AsyncConfig {

    @Bean("mailExecutor")
    public ThreadPoolTaskExecutor mailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数：根据邮件发送频率调整
        executor.setCorePoolSize(5);
        // 最大线程数：防止邮件任务挤占其他异步资源
        executor.setMaxPoolSize(20);
        // 队列容量：缓冲突发邮件请求
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("Mail-Async-");
        // 拒绝策略：由调用线程执行，防止邮件任务被静默丢弃
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
