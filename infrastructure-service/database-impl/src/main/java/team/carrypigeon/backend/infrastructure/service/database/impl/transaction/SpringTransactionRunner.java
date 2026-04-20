package team.carrypigeon.backend.infrastructure.service.database.impl.transaction;

import java.util.function.Supplier;
import org.springframework.transaction.support.TransactionTemplate;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner;

/**
 * Spring 事务运行器。
 * 职责：将 database-api 的事务契约适配到 Spring TransactionTemplate。
 * 边界：Spring 事务对象不向 database-api 暴露。
 */
public class SpringTransactionRunner implements TransactionRunner {

    private final TransactionTemplate transactionTemplate;

    public SpringTransactionRunner(TransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public <T> T runInTransaction(Supplier<T> action) {
        return transactionTemplate.execute(status -> action.get());
    }

    @Override
    public void runInTransaction(Runnable action) {
        transactionTemplate.executeWithoutResult(status -> action.run());
    }
}
