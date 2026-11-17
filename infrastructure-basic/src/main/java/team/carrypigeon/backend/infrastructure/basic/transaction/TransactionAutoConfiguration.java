package team.carrypigeon.backend.infrastructure.basic.transaction;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.support.TransactionTemplate;

/** 在数据库事务模板就绪后装配默认事务运行器，自定义契约实现存在时退出。 */
@AutoConfiguration(afterName = "org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration")
public class TransactionAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(TransactionRunner.class)
    public TransactionRunner transactionRunner(TransactionTemplate template) {
        return new SpringTransactionRunner(template);
    }
}
