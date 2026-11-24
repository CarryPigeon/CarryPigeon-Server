package team.carrypigeon.backend.chat.domain.ws.frame;

import team.carrypigeon.backend.chat.domain.ws.downlink.RealtimeEventType;

/**
 * 服务端下行帧。
 * 职责：定义实时通道全部下行帧的唯一数据模型，供编码器统一转为线上 envelope。
 */
public sealed interface RealtimeFrame {

    /**
     * 事件下发帧。
     *
     * @param eventId 事件 ID，只用于去重，不代表提交顺序；持久化消息事件使用频道游标恢复
     * @param type 事件类型
     * @param serverTime 服务端时间，epoch 毫秒
     * @param payload 事件载荷，由发射方定义
     */
    record Event(String eventId, RealtimeEventType type, long serverTime, Object payload) implements RealtimeFrame {
    }

    /**
     * 命令错误帧。
     *
     * @param requestId 客户端请求 ID
     * @param requestType 客户端命令类型
     * @param code 错误码
     * @param message 面向客户端的错误消息
     */
    record CommandError(String requestId, String requestType, int code, String message) implements RealtimeFrame {
    }

    /**
     * 无错误应答帧：pong、认证应答、续传失败提示等。
     *
     * @param type 帧类型
     * @param requestId 客户端请求 ID，可为空
     * @param data 应答载荷，可为空
     */
    record Ack(String type, String requestId, Object data) implements RealtimeFrame {
    }
}
