package team.carrypigeon.backend.chat.domain.features.message.adaptor.realtime;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 消息事件中的消息体。
 * 职责：以 canonical 字段表达一条消息在实时通道上的投影。
 * 边界：只承载已持久化的消息快照，不携带模型对象。
 *
 * @param mid 消息 ID
 * @param uid 发送者账号 ID
 * @param cid 频道 ID
 * @param domain 消息域
 * @param data 消息域数据
 * @param sendTime 发送时间，epoch 毫秒
 * @param mentions 被提及账号 ID 列表
 * @param preview 消息预览，允许为空
 * @param status 消息状态，小写
 */
public record MessageBody(
        @JsonSerialize(using = ToStringSerializer.class) long mid,
        @JsonSerialize(using = ToStringSerializer.class) long uid,
        @JsonSerialize(using = ToStringSerializer.class) long cid,
        String domain,
        Map<String, Object> data,
        long sendTime,
        @JsonSerialize(contentUsing = ToStringSerializer.class) List<Long> mentions,
        String preview,
        String status
) {

    public MessageBody {
        data = data == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(data));
        mentions = mentions == null ? List.of() : List.copyOf(mentions);
    }
}
