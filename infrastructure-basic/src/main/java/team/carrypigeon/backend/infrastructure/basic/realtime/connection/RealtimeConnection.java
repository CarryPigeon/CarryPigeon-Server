package team.carrypigeon.backend.infrastructure.basic.realtime.connection;

/**
 * 长连接通道的具体细节封装，不暴露通道具体信息
 */
public interface RealtimeConnection {

    /**
     * 发布一个下行帧
     *
     * @param content 发送的内容
     */
    void send(String content);

    /**
     * 是否活跃
     *
     * @return 活跃
     */
    boolean isActive();

    /**
     * 是否可写
     *
     * @return 可写
     */
    boolean isWritable();

    /**
     * 关闭连接
     */
    void close();
}
