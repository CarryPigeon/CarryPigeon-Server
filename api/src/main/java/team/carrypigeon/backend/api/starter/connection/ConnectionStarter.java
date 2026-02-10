package team.carrypigeon.backend.api.starter.connection;

/**
 * 连接层启动器接口。
 * <p>
 * 宿主模块实现该接口以挂接自定义连接服务启动逻辑。
 */
public interface ConnectionStarter {

    /**
     * 启动连接服务。
     */
    void run();
}
