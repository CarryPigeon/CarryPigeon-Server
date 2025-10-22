package team.carrypigeon.backend.api.starter.connection;

/**
 * 连接模块启动器，用于交托给springboot启动连接服务 <br/>
 *
 * 若想要自定义连接服务，请删除connection模块并自定义启动类，然后标记为@PostConstruct注解
 *
 * @author midreamsheep
 * */
public interface ConnectionStarter {
    /**
     * 启动连接服务，请在子方法中使用@PostConstruct注解标记方法
     * */
    void run();
}