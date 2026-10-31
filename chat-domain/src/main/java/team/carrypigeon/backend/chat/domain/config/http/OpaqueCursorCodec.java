package team.carrypigeon.backend.chat.domain.config.http;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemMapping;
import team.carrypigeon.backend.infrastructure.basic.id.IdUtil;

/**
 * 对外分页游标编解码器。
 * 职责：把内部雪花 ID 包装为端点级不透明 cursor，并在入站时做同端点校验。
 * 边界：只处理 HTTP 协议层 cursor，不参与仓储层分页实现；不自行决定游标失败的错误码，由调用方注入。
 */
public final class OpaqueCursorCodec {

    private OpaqueCursorCodec() {
    }

    /**
     * 将内部雪花 ID 编码为带 scope 的不透明游标。
     *
     * @param scope 游标作用域，用于限制端点间不可混用
     * @param snowflakeId 内部雪花 ID
     * @return 对外游标字符串；输入为空时返回空
     */
    public static String encode(String scope, Long snowflakeId) {
        if (snowflakeId == null) {
            return null;
        }
        String raw = scope + ":" + IdUtil.toString(snowflakeId);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 校验并解码外部游标。
     * 输入：预期作用域、客户端提交的游标字符串与调用方所属 feature 的游标错误码。
     * 输出：成功时返回内部雪花 ID，空白游标视为无游标。
     * 失败语义：编码非法、作用域不匹配和 ID 非法都收敛为调用方注入的同一个错误码。
     *
     * @param scope 预期游标作用域
     * @param cursor 客户端提交的游标
     * @param invalidCursorProblem 调用方所属 feature 的游标非法错误码
     * @return 内部雪花 ID；空白游标返回空
     */
    public static Long decode(String scope, String cursor, ProblemMapping invalidCursorProblem) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(cursor.trim()), StandardCharsets.UTF_8);
            int separatorIndex = decoded.indexOf(':');
            if (separatorIndex <= 0) {
                throw ProblemException.of(invalidCursorProblem);
            }
            String actualScope = decoded.substring(0, separatorIndex);
            if (!scope.equals(actualScope)) {
                throw ProblemException.of(invalidCursorProblem);
            }
            long parsed = Long.parseLong(decoded.substring(separatorIndex + 1));
            if (parsed <= 0L) {
                throw ProblemException.of(invalidCursorProblem);
            }
            return parsed;
        } catch (IllegalArgumentException exception) {
            throw ProblemException.of(invalidCursorProblem);
        }
    }
}
