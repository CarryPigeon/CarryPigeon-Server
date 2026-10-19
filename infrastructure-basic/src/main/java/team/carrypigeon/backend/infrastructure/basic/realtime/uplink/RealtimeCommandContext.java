package team.carrypigeon.backend.infrastructure.basic.realtime.uplink;

import team.carrypigeon.backend.infrastructure.basic.realtime.downlink.RealtimeFrame;

/**
 * 实时命令处理上下文。
 * 职责：向命令处理器暴露"当前连接是谁"与"如何回复"，隐藏传输层细节。
 * 边界：不暴露具体连接对象、不暴露关闭语义；关闭决策属于分派层。
 */
public interface RealtimeCommandContext {

    /**
     * 当前连接账号 ID。
     * 约束：仅在处理器声明需要认证时可用。
     *
     * @return 账号 ID
     */
    long accountId();

    /**
     * 回复一个下行帧。
     *
     * @param frame 待下发帧
     */
    void reply(RealtimeFrame frame);

    /**
     * 客户端在认证帧里声明的最后事件 ID。
     *
     * @return 最后事件 ID，未声明时为 null
     */
    String lastEventId();
}
