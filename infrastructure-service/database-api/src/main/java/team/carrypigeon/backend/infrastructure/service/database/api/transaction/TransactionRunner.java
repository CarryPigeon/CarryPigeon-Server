package team.carrypigeon.backend.infrastructure.service.database.api.transaction;

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
}
