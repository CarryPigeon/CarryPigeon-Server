package team.carrypigeon.backend.chat.domain.cmp.api.message;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemReason;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemException;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeMessageKeys;
import team.carrypigeon.backend.chat.domain.controller.web.api.dto.MessageListRequest;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;

import java.util.List;
import java.util.Map;

/**
 * 消息列表查询绑定节点。
 * <p>
 * 解析消息列表接口中的频道、游标与分页参数并写入上下文。
 */
@Slf4j
@LiteflowComponent("ApiMessageListBind")
public class ApiMessageListBindNode extends CPNodeComponent {

    /** 列表最大分页条数（防止一次拉取过大）。 */
    private static final int MAX_LIMIT = 50;
    /** 默认分页条数。 */
    private static final int DEFAULT_LIMIT = 50;
    /** cursor 前缀（兼容形态，例如：msg_7231556...）。 */
    private static final String CURSOR_PREFIX = "msg_";

    /**
     * 解析并绑定消息列表请求。
     *
     * <p>依赖上下文：{@link CPFlowKeys#REQUEST}（类型必须为 {@link MessageListRequest}）。
     *
     * <p>输出：写入 {@code cid/cursorMid/limit}（见类注释）。
     *
     * <p>失败语义：参数非法时抛出 {@link CPProblemException}（HTTP 422）。
     */
    @Override
    protected void process(CPFlowContext context) {
        Object reqObj = context.get(CPFlowKeys.REQUEST);
        if (!(reqObj instanceof MessageListRequest req)) {
            throw new CPProblemException(CPProblem.of(CPProblemReason.VALIDATION_FAILED, "validation failed"));
        }
        long cid = parseId(req.cid(), "cid");
        int limit = req.limit() == null ? DEFAULT_LIMIT : req.limit();
        if (limit <= 0) {
            limit = DEFAULT_LIMIT;
        }
        limit = Math.min(MAX_LIMIT, limit);

        long startTime = 0L;
        if (req.cursor() != null && !req.cursor().isBlank()) {
            startTime = parseCursor(req.cursor());
        }

        context.set(CPNodeChannelKeys.CHANNEL_INFO_ID, cid);
        context.set(CPNodeMessageKeys.MESSAGE_LIST_CURSOR_MID, startTime);
        context.set(CPNodeMessageKeys.MESSAGE_LIST_COUNT, limit);

        log.debug("消息列表绑定：完成：cid={}, cursorMid={}, limit={}", cid, startTime, limit);
    }

    /**
     * 解析必填雪花 ID（十进制字符串）。
     */
    private long parseId(String str, String field) {
        try {
            return Long.parseLong(str);
        } catch (Exception e) {
            throw new CPProblemException(CPProblem.of(CPProblemReason.VALIDATION_FAILED, "validation failed",
                    Map.of("field_errors", List.of(
                            Map.of("field", field, "reason", "invalid", "message", "invalid id")
                    ))));
        }
    }

    /**
     * 解析 cursor（当前实现以最后一条消息的 mid 作为游标）。
     *
     * <p>兼容形态：
     * <ul>
     *   <li>{@code "723155640365318144"}</li>
     *   <li>{@code "msg_723155640365318144"}</li>
     * </ul>
     *
     * @param cursor cursor 字符串
     * @return 解码后的 mid
     */
    private long parseCursor(String cursor) {
        String raw = cursor.trim();
        if (raw.startsWith(CURSOR_PREFIX)) {
            raw = raw.substring(CURSOR_PREFIX.length());
        }
        try {
            return Long.parseLong(raw);
        } catch (Exception e) {
            throw new CPProblemException(CPProblem.of(CPProblemReason.CURSOR_INVALID, "cursor invalid"));
        }
    }
}
