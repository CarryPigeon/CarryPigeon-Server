package team.carrypigeon.backend.infrastructure.service.mail.impl.config;

import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/** 邮件专用有界执行器；饱和或关闭时明确拒绝，不允许请求线程接管 SMTP 工作。 */
@Configuration
public class AsyncConfig {

    @Bean("mailExecutor")
    @ConditionalOnMissingBean(name = "mailExecutor")
    public ThreadPoolTaskExecutor mailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数：根据邮件发送频率调整
        executor.setCorePoolSize(5);
        // 最大线程数：防止邮件任务挤占其他异步资源
        executor.setMaxPoolSize(20);
        // 队列容量：缓冲突发邮件请求
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("Mail-Async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        // 正常关闭最多等待 30 秒排空任务，匹配分发包停止窗口；未完成任务和异常退出不提供恢复保证。
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        return executor;
    }
}
