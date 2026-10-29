package team.carrypigeon.backend.infrastructure.basic.realtime;

import org.springframework.stereotype.Component;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;
import team.carrypigeon.backend.infrastructure.basic.realtime.downlink.RealtimeFrame;
import team.carrypigeon.backend.infrastructure.basic.realtime.uplink.RealtimeClientMessage;

/**
 * 实时通道帧编解码器。
 * 职责：把下行帧编码为唯一形态的线上 envelope，解析并校验上行 envelope，同时固化线上协议字面量。
 */
@Component
public final class RealtimeFrameCodec {

    /** 事件下发帧的帧类型。 */
    public static final String EVENT_FRAME_TYPE = "event";

    /** 无法归属到具体命令时使用的帧类型前缀。 */
    public static final String COMMAND_FRAME_TYPE = "command";

    /** 命令错误帧在请求类型后追加的后缀。 */
    public static final String ERROR_TYPE_SUFFIX = ".err";

    private final JsonProvider jsonProvider;

    public RealtimeFrameCodec(JsonProvider jsonProvider) {
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
                    error.requestType() + ERROR_TYPE_SUFFIX,
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
     * 失败语义：envelope 缺失或 type 缺失、空白时抛出 {@link RealtimeProtocolException}。
     *
     * @param message 客户端消息
     */
    public void validate(RealtimeClientMessage message) {
        if (message == null) {
            throw new RealtimeProtocolException("frame_invalid", "frame must be an object");
        }
        if (message.type() == null || message.type().isBlank()) {
            throw new RealtimeProtocolException("type_blank", "type must not be blank");
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
}
