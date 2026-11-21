package team.carrypigeon.backend.chat.domain.features.channel.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/** 频道清理使用独立的进程内队列，避免占用邮件发送线程。 */
@Configuration
public class ChannelDeletionExecutorConfiguration {

    @Bean("channelDeletionExecutor")
    public ThreadPoolTaskExecutor channelDeletionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("Channel-Cleanup-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        return executor;
    }
}
