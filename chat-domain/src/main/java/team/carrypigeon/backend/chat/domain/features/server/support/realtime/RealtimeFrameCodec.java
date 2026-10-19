package team.carrypigeon.backend.chat.domain.features.server.support.realtime;

import team.carrypigeon.backend.chat.domain.features.server.domain.api.realtime.RealtimeClientMessage;
import team.carrypigeon.backend.chat.domain.features.server.domain.api.realtime.RealtimeFrame;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProviderImpl;

/**
 * 实时通道帧编解码器。
 * 职责：把下行帧编码为唯一形态的线上 envelope，并解析、校验客户端 envelope。
 * 边界：不读取通道状态、不执行认证或续传；不产生 Netty 对象，编解码结果是纯文本。
 */
public final class RealtimeFrameCodec {

    /** 下行 envelope 的固定类型键：事件帧。 */
    public static final String EVENT_FRAME_TYPE = "event";

    private final JsonProviderImpl jsonProvider;

    public RealtimeFrameCodec(JsonProviderImpl jsonProvider) {
        this.jsonProvider = jsonProvider;
    }

    /**
     * 编码一个下行帧。
     * 输出：四个键固定为 type、id、data、error 的 JSON 文本，空值输出 null 而不省略。
     *
     * @param frame 下行帧
     * @return JSON 文本
     */
    public String encode(RealtimeFrame frame) {
        return switch (frame) {
            case RealtimeFrame.Event event -> jsonProvider.toJson(new Envelope(
                    EVENT_FRAME_TYPE,
                    null,
                    new EventBody(
                            event.eventId(),
                            event.type().wireName(),
                            event.serverTime(),
                            event.payload()
                    ),
                    null
            ));
            case RealtimeFrame.Ack ack -> jsonProvider.toJson(new Envelope(
                    ack.type(),
                    ack.requestId(),
                    ack.data(),
                    null
            ));
            case RealtimeFrame.CommandError error -> jsonProvider.toJson(new Envelope(
                    error.requestType() + ".err",
                    error.requestId(),
                    null,
                    new ErrorBody(error.reason(), error.message())
            ));
        };
    }

    /**
     * 解析客户端 envelope。
     *
     * @param text 客户端文本帧
     * @return 客户端消息
     */
    public RealtimeClientMessage decode(String text) {
        return jsonProvider.fromJson(text, RealtimeClientMessage.class);
    }

    /**
     * 校验客户端 envelope 的最小结构。
     * 失败语义：type 缺失或空白时抛出 {@link InvalidClientFrameException}。
     *
     * @param message 客户端消息
     */
    public void validate(RealtimeClientMessage message) {
        if (message == null) {
            throw new InvalidClientFrameException("frame_invalid", "frame must be an object");
        }
        if (message.type() == null || message.type().isBlank()) {
            throw new InvalidClientFrameException("type_blank", "type must not be blank");
        }
    }

    /**
     * 下行 envelope 结构。
     *
     * @param type 帧类型
     * @param id 请求 ID
     * @param data 成功载荷
     * @param error 错误载荷
     */
    private record Envelope(String type, String id, Object data, Object error) {
    }

    /**
     * 事件帧载荷。
     *
     * @param eventId 事件 ID
     * @param eventType 事件类型名
     * @param serverTime 服务端时间，epoch 毫秒
     * @param payload 事件载荷
     */
    private record EventBody(String eventId, String eventType, long serverTime, Object payload) {
    }

    /**
     * 错误帧载荷。
     *
     * @param reason 稳定错误原因
     * @param message 面向客户端的错误消息
     */
    private record ErrorBody(String reason, String message) {
    }

    /**
     * 客户端 envelope 结构无效。
     * 职责：携带内部调试原因与对外校验消息，供分派层决定是否关闭连接。
     */
    public static final class InvalidClientFrameException extends RuntimeException {

        private final String reason;

        public InvalidClientFrameException(String reason, String message) {
            super(message);
            this.reason = reason;
        }

        public String reason() {
            return reason;
        }
    }
}
