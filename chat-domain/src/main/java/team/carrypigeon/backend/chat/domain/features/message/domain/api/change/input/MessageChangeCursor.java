package team.carrypigeon.backend.chat.domain.features.message.domain.api.change.input;

import java.util.UUID;

/**
 * 消息变更的恢复位置，同时用于请求和返回结果。
 *
 * @param instanceId 产生游标的服务实例标识
 * @param channelId 游标所属频道 ID
 * @param sequence 已读取的事件位置
 * @param issuedAt 游标起始时间，Unix 时间戳，单位为毫秒
 */
public record MessageChangeCursor(UUID instanceId, long channelId, long sequence, long issuedAt) {
}
