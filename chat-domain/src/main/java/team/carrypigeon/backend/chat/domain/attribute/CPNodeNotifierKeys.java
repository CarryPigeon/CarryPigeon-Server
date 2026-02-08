package team.carrypigeon.backend.chat.domain.attribute;

import com.fasterxml.jackson.databind.JsonNode;
import team.carrypigeon.backend.api.chat.domain.flow.CPKey;

import java.util.Set;

/**
 * 通知组件相关上下文 key（Notifier_*）。
 */
public final class CPNodeNotifierKeys {

    /** {@code Set<Long>}: 需要被通知的用户 id 集合。 */
    public static final CPKey<Set> NOTIFIER_UIDS = CPKey.of("Notifier_Uids", Set.class);

    /** {@code JsonNode}: 通知 payload（由 route 定义其结构）。 */
    public static final CPKey<JsonNode> NOTIFIER_DATA = CPKey.of("Notifier_Data", JsonNode.class);

    private CPNodeNotifierKeys() {
    }
}
