package team.carrypigeon.backend.api.connection.pool;

/**
 * CarryPigeon的Channel接口，用于解耦不同模块之间的依赖关系
 * @author midreamsheep
 * */
public interface CPChannel {
    /**
     * 向客户端写出数据
     * @param msg 具体下消息类型，仅支持字符串，且应为json格式
     * */
    void write(String msg);
    /**
     * TODO
     * 获取当前连接的私有数据
     * @author midreamsheep
     * */
    void getData();
}