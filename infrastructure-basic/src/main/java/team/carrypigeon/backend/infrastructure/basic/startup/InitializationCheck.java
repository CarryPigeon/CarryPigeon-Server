package team.carrypigeon.backend.infrastructure.basic.startup;

/**
 * 初始化检查契约。
 * 职责：为启动层提供统一的检查入口，让各基础设施实现模块可参与启动期自检。
 * 边界：这里只定义共享检查协议，不承载任何具体外部服务检查逻辑。
 */
public interface InitializationCheck {

    /**
     * @return 当前检查的稳定名称，用于日志和失败定位
     */
    String name();

    /**
     * @return 当前检查是否为必需项；必需项失败会阻止应用继续启动
     */
    default boolean required() {
        return true;
    }

    /**
     * 执行当前初始化检查。
     *
     * @return 初始化检查结果
     */
    InitializationCheckResult check();
}
