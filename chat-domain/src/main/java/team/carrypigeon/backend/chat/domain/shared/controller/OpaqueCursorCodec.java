package team.carrypigeon.backend.chat.domain.shared.controller;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.id.Ids;

/**
 * 对外分页游标编解码器。
 * 职责：把内部雪花 ID 包装为端点级不透明 cursor，并在入站时做同端点校验。
 * 边界：只处理 HTTP 协议层 cursor，不参与仓储层分页实现。
 */
public final class OpaqueCursorCodec {

    private OpaqueCursorCodec() {
    }

    public static String encode(String scope, Long snowflakeId) {
        if (snowflakeId == null) {
            return null;
        }
        String raw = scope + ":" + Ids.toString(snowflakeId);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static Long decode(String scope, String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(cursor.trim()), StandardCharsets.UTF_8);
            int separatorIndex = decoded.indexOf(':');
            if (separatorIndex <= 0) {
                throw invalidCursor();
            }
            String actualScope = decoded.substring(0, separatorIndex);
            if (!scope.equals(actualScope)) {
                throw invalidCursor();
            }
            long parsed = Long.parseLong(decoded.substring(separatorIndex + 1));
            if (parsed <= 0L) {
                throw invalidCursor();
            }
            return parsed;
        } catch (IllegalArgumentException exception) {
            throw invalidCursor();
        }
    }

    private static ProblemException invalidCursor() {
        return ProblemException.validationFailed("cursor_invalid", "cursor is invalid");
    }
}
