package team.carrypigeon.backend.infrastructure.basic.realtime.downlink;

/**
 * 实时事件类型标识。
 * 职责：承载线上事件名，作为事件的唯一定性标识。
 *
 * @param wireName 线上事件名，用于给客户端判断帧类型
 */
public record RealtimeEventType(String wireName) {}
