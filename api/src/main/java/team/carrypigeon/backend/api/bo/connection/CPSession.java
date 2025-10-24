package team.carrypigeon.backend.api.bo.connection;

/**
 * CarryPigeon通道包装类，用于包装通道用于不同模块之间的解耦
 * @author midreamsheep
 * */
public interface CPSession {
    /**
     * 通过通道发送消息
     * @param msg 要发送的消息,为json格式
     * */
    default void write(String msg){
        write(msg,true);
    }
    /**
     * 通过通道发送消息
     * @param msg 要发送的消息,为json格式
     * @param encrypted 是否加密
     * */
    void write(String msg,boolean encrypted);
    /**
     * 通道内应该维护一个map <br/>
     * 通过key获取对应的属性值 <br/>
     * @param key 属性key
     * */
    <T> T getAttributeValue(String key, Class<T> type);
    /**
     * 将属性值设置到通道内
     * @param key 属性key
     * @param value 属性值
     * */
    void setAttributeValue(String key, Object value);

    /**
     * 关闭通道
     * */
    void close();
}