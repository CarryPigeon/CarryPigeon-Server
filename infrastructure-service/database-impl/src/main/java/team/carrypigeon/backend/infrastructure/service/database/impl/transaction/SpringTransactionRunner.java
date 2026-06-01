package team.carrypigeon.backend.infrastructure.service.database.impl.transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
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

    /**
     * 在 Spring 事务上下文中执行并返回结果。
     */
    @Override
    public <T> T runInTransaction(Supplier<T> action) {
        return transactionTemplate.execute(status -> action.get());
    }

    /**
     * 在 Spring 事务上下文中执行无返回值动作。
     */
    @Override
    public void runInTransaction(Runnable action) {
        transactionTemplate.executeWithoutResult(status -> action.run());
    }

    /**
     * 在 Spring 事务中执行，并把提交后副作用绑定到事务同步生命周期。
     * 约束：若当前事务未启用同步，则退化为在事务回调成功返回后执行。
     */
    @Override
    public <T> T runInTransaction(TransactionalAction<T> action) {
        List<Runnable> fallbackAfterCommitActions = new ArrayList<>();
        T result = transactionTemplate.execute(status -> action.run(afterCommitAction ->
                registerAfterCommitAction(afterCommitAction, fallbackAfterCommitActions)));
        for (Runnable fallbackAfterCommitAction : List.copyOf(fallbackAfterCommitActions)) {
            fallbackAfterCommitAction.run();
        }
        return result;
    }

    /**
     * 在 Spring 事务中执行无返回值动作，并支持提交后副作用登记。
     */
    @Override
    public void runInTransaction(TransactionalRunnable action) {
        runInTransaction(afterCommit -> {
            action.run(afterCommit);
            return null;
        });
    }

    private void registerAfterCommitAction(Runnable action, List<Runnable> fallbackAfterCommitActions) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
            return;
        }
        fallbackAfterCommitActions.add(action);
    }
}
