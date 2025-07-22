package team.carrypigeon.backend.api.connection.pool;

/**
 * 连接池启动器，一个服务器只应该拥有一个启动器
 * 用于解耦连接管理模块与启动器模块，标准实现使用netty为连接框架
 * */
@FunctionalInterface
public interface ConnectionPoolStarter {
    /**
     * 启动
     * @param port 连接端口
     * */
    void run(int port);
}