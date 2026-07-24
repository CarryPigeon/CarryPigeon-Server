package team.carrypigeon.backend.chat.domain.features.plugin.domain.projection;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 插件校验后的 canonical 消息内容。
 */
public record ValidatedMessageDataResult(
        String domain,
        String domainVersion,
        Map<String, Object> data,
        String preview
) {

    public ValidatedMessageDataResult {
        data = data == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(data));
        preview = preview == null ? "" : preview;
    }
}
