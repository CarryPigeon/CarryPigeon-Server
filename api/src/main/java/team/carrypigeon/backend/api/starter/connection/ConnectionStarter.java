package team.carrypigeon.backend.api.starter.connection;

/**
 * 连接模块启动器，用于交托给springboot启动连接服务 <br/>
 *
 * springboot初始化完成后将会调用该模块的run方法启动连接服务 <br/>
 *
 * 若想要自定义连接服务，请删除connection模块并自定义启动类，然后注入springboot
 *
 * @author midreamsheep
 * */
@FunctionalInterface
public interface ConnectionStarter {
    /**
     * 启动连接服务
     * @param config 连接配置，存储连接端口等相关信息
     * */
    void run(ConnectionConfig config);
}