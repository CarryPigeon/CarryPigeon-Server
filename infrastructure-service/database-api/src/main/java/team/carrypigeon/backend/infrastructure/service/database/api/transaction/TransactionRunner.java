package team.carrypigeon.backend.infrastructure.service.database.api.transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 数据库事务运行抽象。
 * 职责：让上层模块以数据库服务契约运行事务逻辑。
 * 边界：不暴露 Spring TransactionTemplate、JDBC 或具体数据源类型。
 */
public interface TransactionRunner {

    /**
     * 在事务边界内执行有返回值的逻辑。
     *
     * @param action 需要事务保护的动作
     * @param <T> 返回值类型
     * @return 动作执行结果
     */
    <T> T runInTransaction(Supplier<T> action);

    /**
     * 在事务边界内执行无返回值的逻辑。
     *
     * @param action 需要事务保护的动作
     */
    void runInTransaction(Runnable action);

    /**
     * 在事务边界内执行逻辑，并允许注册仅在提交成功后触发的副作用。
     * 约束：若事务回滚或动作抛错，已注册的 after-commit 动作不得执行。
     *
     * @param action 需要事务保护的动作
     * @param <T> 返回值类型
     * @return 动作执行结果
     */
    default <T> T runInTransaction(TransactionalAction<T> action) {
        List<Runnable> afterCommitActions = new ArrayList<>();
        T result = runInTransaction(() -> action.run(afterCommitActions::add));
        runAfterCommitActions(afterCommitActions);
        return result;
    }

    /**
     * 在事务边界内执行无返回值逻辑，并允许注册仅在提交成功后触发的副作用。
     *
     * @param action 需要事务保护的动作
     */
    default void runInTransaction(TransactionalRunnable action) {
        runInTransaction(afterCommit -> {
            action.run(afterCommit);
            return null;
        });
    }

    private static void runAfterCommitActions(List<Runnable> afterCommitActions) {
        for (Runnable afterCommitAction : List.copyOf(afterCommitActions)) {
            afterCommitAction.run();
        }
    }

    /**
     * 事务成功提交后动作注册器。
     * 职责：供上层在事务体内登记提交后副作用。
     * 边界：只表达“提交后执行”语义，不扩展为通用事件总线。
     */
    @FunctionalInterface
    interface AfterCommitExecutor {

        /**
         * 注册一条事务成功提交后执行的动作。
         *
         * @param action 提交后动作
         */
        void execute(Runnable action);
    }

    /**
     * 带返回值的事务动作。
     *
     * @param <T> 返回值类型
     */
    @FunctionalInterface
    interface TransactionalAction<T> {

        /**
         * 在事务内执行动作，并可登记提交后副作用。
         *
         * @param afterCommit 提交后动作注册器
         * @return 动作结果
         */
        T run(AfterCommitExecutor afterCommit);
    }

    /**
     * 无返回值的事务动作。
     */
    @FunctionalInterface
    interface TransactionalRunnable {

        /**
         * 在事务内执行动作，并可登记提交后副作用。
         *
         * @param afterCommit 提交后动作注册器
         */
        void run(AfterCommitExecutor afterCommit);
    }
}
