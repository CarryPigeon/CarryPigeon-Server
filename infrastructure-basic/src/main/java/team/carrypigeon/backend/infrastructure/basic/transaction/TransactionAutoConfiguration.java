package team.carrypigeon.backend.infrastructure.basic.transaction;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 事务能力自动配置。
 * 职责：在存在事务管理器的运行时装配统一事务执行器。
 * 边界：只依赖 Spring 事务抽象，不感知数据库、ORM 或具体数据源；没有任何事务管理器时不装配。
 */
@AutoConfiguration(afterName = "org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration")
public class TransactionAutoConfiguration {

    /**
     * 装配事务执行器。
     *
     * @param transactionManager Spring 事务管理器
     * @return 供上层模块使用的事务执行器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(PlatformTransactionManager.class)
    public TransactionRunner transactionRunner(PlatformTransactionManager transactionManager) {
        return new SpringTransactionRunner(new TransactionTemplate(transactionManager));
    }
}
