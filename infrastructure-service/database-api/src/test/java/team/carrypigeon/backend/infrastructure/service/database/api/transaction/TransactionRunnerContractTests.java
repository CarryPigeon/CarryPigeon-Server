package team.carrypigeon.backend.infrastructure.service.database.api.transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * `TransactionRunner` 默认时序契约测试。
 * 职责：验证提交后动作只在事务体成功返回后执行，并覆盖有返回值与无返回值入口。
 * 边界：不验证 Spring 事务同步器或具体数据库实现。
 */
@Tag("contract")
class TransactionRunnerContractTests {

    /**
     * 验证有返回值事务先完成事务体，再按注册顺序执行提交后动作。
     */
    @Test
    @DisplayName("transactional action success runs after commit actions in order")
    void runInTransaction_transactionalActionSuccess_runsAfterCommitActionsInOrder() {
        TransactionRunner runner = new InlineTransactionRunner();
        List<String> lifecycle = new ArrayList<>();

        String result = runner.runInTransaction((TransactionRunner.TransactionalAction<String>) afterCommit -> {
            lifecycle.add("transaction");
            afterCommit.execute(() -> lifecycle.add("after-1"));
            afterCommit.execute(() -> lifecycle.add("after-2"));
            return "result";
        });

        assertEquals("result", result);
        assertEquals(List.of("transaction", "after-1", "after-2"), lifecycle);
    }

    /**
     * 验证事务体抛错时已登记的提交后动作不会执行。
     */
    @Test
    @DisplayName("transactional action failure skips after commit actions")
    void runInTransaction_transactionalActionFailure_skipsAfterCommitActions() {
        TransactionRunner runner = new InlineTransactionRunner();
        List<String> lifecycle = new ArrayList<>();

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> runner.runInTransaction((TransactionRunner.TransactionalAction<Void>) afterCommit -> {
                    afterCommit.execute(() -> lifecycle.add("after"));
                    throw new IllegalStateException("rollback");
                })
        );

        assertEquals("rollback", exception.getMessage());
        assertEquals(List.of(), lifecycle);
    }

    /**
     * 验证无返回值事务入口复用相同的提交后时序。
     */
    @Test
    @DisplayName("transactional runnable success runs after commit action")
    void runInTransaction_transactionalRunnableSuccess_runsAfterCommitAction() {
        TransactionRunner runner = new InlineTransactionRunner();
        List<String> lifecycle = new ArrayList<>();

        runner.runInTransaction((TransactionRunner.TransactionalRunnable) afterCommit -> {
            lifecycle.add("transaction");
            afterCommit.execute(() -> lifecycle.add("after"));
        });

        assertEquals(List.of("transaction", "after"), lifecycle);
    }

    /**
     * `InlineTransactionRunner` 测试替身。
     * 职责：同步执行基础事务入口，使测试只观察接口默认编排时序。
     */
    private static final class InlineTransactionRunner implements TransactionRunner {

        @Override
        public <T> T runInTransaction(Supplier<T> action) {
            return action.get();
        }

        @Override
        public void runInTransaction(Runnable action) {
            action.run();
        }
    }
}
