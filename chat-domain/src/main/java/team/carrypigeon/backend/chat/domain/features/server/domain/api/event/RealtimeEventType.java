package team.carrypigeon.backend.chat.domain.features.server.domain.api.event;

/**
 * 实时事件类型标识。
 * 职责：承载线上事件名，作为事件的唯一定性标识。
 * 边界：只有名字，不携带投递策略或分类；新增事件类型不需要修改本类，由发射方自行定义常量。
 *
 * @param wireName 线上事件名，必须匹配 {@code [a-z0-9._-]+}
 */
public record RealtimeEventType(String wireName) {

    public RealtimeEventType {
        if (wireName == null || wireName.isBlank()) {
            throw new IllegalArgumentException("wireName must not be blank");
        }
        if (!wireName.matches("[a-z0-9._-]+")) {
            throw new IllegalArgumentException("wireName must match [a-z0-9._-]+");
        }
    }
}
