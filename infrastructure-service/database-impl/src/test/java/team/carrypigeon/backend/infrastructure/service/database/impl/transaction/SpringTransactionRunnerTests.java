package team.carrypigeon.backend.infrastructure.service.database.impl.transaction;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * SpringTransactionRunner 契约测试。
 * 职责：验证 database-api 事务契约对 Spring TransactionTemplate 的适配行为。
 * 边界：同时覆盖 mock 回调委派语义与真实 Spring 事务同步激活语义，不验证真实数据库事务。
 */
@Tag("contract")
class SpringTransactionRunnerTests {

    /**
     * 验证带返回值的事务动作会通过 TransactionTemplate 执行并返回结果。
     */
    @Test
    @DisplayName("run in transaction supplier delegates to transaction template")
    void runInTransaction_supplier_delegatesToTransactionTemplate() {
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            TransactionCallback<String> callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        });
        SpringTransactionRunner runner = new SpringTransactionRunner(transactionTemplate);

        String result = runner.runInTransaction(() -> "done");

        assertEquals("done", result);
    }

    /**
     * 验证无返回值的事务动作会通过 TransactionTemplate 执行。
     */
    @Test
    @DisplayName("run in transaction runnable delegates to transaction template")
    void runInTransaction_runnable_delegatesToTransactionTemplate() {
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        AtomicBoolean executed = new AtomicBoolean(false);
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            java.util.function.Consumer<TransactionStatus> action = invocation.getArgument(0);
            action.accept(mock(TransactionStatus.class));
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
        SpringTransactionRunner runner = new SpringTransactionRunner(transactionTemplate);

        runner.runInTransaction(() -> executed.set(true));

        assertTrue(executed.get());
    }

    /**
     * 验证未启用真实事务同步时，登记的 after-commit 动作会在事务回调成功返回后执行。
     */
    @Test
    @DisplayName("run in transaction transactional action executes fallback after commit callback")
    void runInTransaction_transactionalAction_executesFallbackAfterCommitCallback() {
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        StringBuilder lifecycle = new StringBuilder();
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            TransactionCallback<String> callback = invocation.getArgument(0);
            String result = callback.doInTransaction(mock(TransactionStatus.class));
            lifecycle.append("|transaction-complete");
            return result;
        });
        SpringTransactionRunner runner = new SpringTransactionRunner(transactionTemplate);

        String result = runner.runInTransaction(afterCommit -> {
            lifecycle.append("transaction-body");
            afterCommit.execute(() -> lifecycle.append("|after-commit"));
            return "done";
        });

        assertEquals("done", result);
        assertEquals("transaction-body|transaction-complete|after-commit", lifecycle.toString());
    }

    /**
     * 验证真实 Spring 事务同步激活时，after-commit 动作会绑定到事务同步生命周期。
     */
    @Test
    @DisplayName("run in transaction transactional action with real spring synchronization executes after commit lifecycle")
    void runInTransaction_transactionalActionWithRealSpringSynchronization_executesAfterCommitLifecycle() {
        RecordingPlatformTransactionManager transactionManager = new RecordingPlatformTransactionManager();
        SpringTransactionRunner runner = new SpringTransactionRunner(new TransactionTemplate(transactionManager));
        StringBuilder lifecycle = new StringBuilder();
        AtomicBoolean synchronizationActive = new AtomicBoolean(false);

        String result = runner.runInTransaction(afterCommit -> {
            synchronizationActive.set(TransactionSynchronizationManager.isSynchronizationActive());
            lifecycle.append("transaction-body");
            afterCommit.execute(() -> lifecycle.append("|after-commit"));
            lifecycle.append("|transaction-body-end");
            return "done";
        });
        lifecycle.append("|after-return");

        assertEquals("done", result);
        assertTrue(synchronizationActive.get());
        assertTrue(transactionManager.committed);
        assertFalse(transactionManager.rolledBack);
        assertEquals("transaction-body|transaction-body-end|after-commit|after-return", lifecycle.toString());
    }

    /**
     * 验证事务动作失败时不会执行已登记的 after-commit 动作。
     */
    @Test
    @DisplayName("run in transaction transactional action failure skips after commit callback")
    void runInTransaction_transactionalActionFailure_skipsAfterCommitCallback() {
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        AtomicBoolean afterCommitExecuted = new AtomicBoolean(false);
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            TransactionCallback<String> callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        });
        SpringTransactionRunner runner = new SpringTransactionRunner(transactionTemplate);

        assertThrows(IllegalStateException.class, () -> runner.runInTransaction(afterCommit -> {
            afterCommit.execute(() -> afterCommitExecuted.set(true));
            throw new IllegalStateException("transaction failed");
        }));

        assertFalse(afterCommitExecuted.get());
    }

    /**
     * 验证真实 Spring 事务回滚时，不会触发 after-commit 回调。
     */
    @Test
    @DisplayName("run in transaction transactional action rollback with real spring synchronization skips after commit callback")
    void runInTransaction_transactionalActionRollbackWithRealSpringSynchronization_skipsAfterCommitCallback() {
        RecordingPlatformTransactionManager transactionManager = new RecordingPlatformTransactionManager();
        SpringTransactionRunner runner = new SpringTransactionRunner(new TransactionTemplate(transactionManager));
        AtomicBoolean afterCommitExecuted = new AtomicBoolean(false);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> runner.runInTransaction(afterCommit -> {
            afterCommit.execute(() -> afterCommitExecuted.set(true));
            throw new IllegalStateException("transaction failed");
        }));

        assertEquals("transaction failed", exception.getMessage());
        assertFalse(afterCommitExecuted.get());
        assertFalse(transactionManager.committed);
        assertTrue(transactionManager.rolledBack);
    }

    private static final class RecordingPlatformTransactionManager extends AbstractPlatformTransactionManager {

        private boolean committed;
        private boolean rolledBack;

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
            committed = true;
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
            rolledBack = true;
        }
    }
}
