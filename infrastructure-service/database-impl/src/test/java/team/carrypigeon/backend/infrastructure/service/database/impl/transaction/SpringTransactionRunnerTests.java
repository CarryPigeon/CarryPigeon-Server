package team.carrypigeon.backend.infrastructure.service.database.impl.transaction;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * SpringTransactionRunner 契约测试。
 * 职责：验证 database-api 事务契约对 Spring TransactionTemplate 的适配行为。
 * 边界：不验证真实数据库事务，只验证回调委派语义。
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
}
