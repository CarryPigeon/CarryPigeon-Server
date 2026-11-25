package team.carrypigeon.backend.infrastructure.service.database.memory;

import static team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent.TEST;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.SmartTransactionObject;
import org.springframework.transaction.support.TransactionTemplate;
import team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent;

/** 内存数据库的 Spring 事务管理器。事务持有所有表共用的锁，回滚时恢复事务开始前的快照。 */
@EnvironmentComponent(profiles = TEST)
public class MemoryDatabase extends AbstractPlatformTransactionManager {
    private final ReentrantLock lock = new ReentrantLock();
    private final List<Supplier<Runnable>> snapshots = new ArrayList<>();
    private final ThreadLocal<Transaction> current = new ThreadLocal<>();

    /** 嵌套的 REQUIRED 调用参与同一事务；内层失败时由 Spring 在外层提交前检查回滚标记。 */
    private static final class Transaction implements SmartTransactionObject {
        List<Runnable> restore = List.of();
        boolean rollbackOnly;

        @Override
        public boolean isRollbackOnly() {
            return rollbackOnly;
        }
    }

    void register(Supplier<Runnable> snapshot) {
        locked(() -> snapshots.add(snapshot));
    }

    /** 单次表操作和跨表事务使用同一把可重入锁，避免读到其他线程未提交的数据。 */
    <T> T locked(Supplier<T> action) {
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }

    <T> T runInTransaction(Supplier<T> action) {
        return new TransactionTemplate(this).execute(status -> action.get());
    }

    @Override
    protected Object doGetTransaction() {
        Transaction transaction = current.get();
        return transaction == null ? new Transaction() : transaction;
    }

    @Override
    protected boolean isExistingTransaction(Object transaction) {
        return current.get() == transaction;
    }

    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) {
        lock.lock();
        try {
            Transaction state = (Transaction) transaction;
            state.restore = snapshots.stream().map(Supplier::get).toList();
            current.set(state);
        } catch (RuntimeException | Error failure) {
            lock.unlock();
            throw failure;
        }
    }

    @Override
    protected void doSetRollbackOnly(DefaultTransactionStatus status) {
        ((Transaction) status.getTransaction()).rollbackOnly = true;
    }

    @Override
    protected void doCommit(DefaultTransactionStatus status) {
        // 数据已在持锁期间写入表，提交后释放锁即可对其他线程可见。
    }

    @Override
    protected void doRollback(DefaultTransactionStatus status) {
        ((Transaction) status.getTransaction()).restore.forEach(Runnable::run);
    }

    @Override
    protected void doCleanupAfterCompletion(Object transaction) {
        current.remove();
        lock.unlock();
    }
}
