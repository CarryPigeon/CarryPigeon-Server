package team.carrypigeon.backend.chat.domain.cmp.api.message;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemReason;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemException;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeMessageKeys;
import team.carrypigeon.backend.chat.domain.controller.web.api.dto.MessageDeleteRequest;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;

import java.util.List;
import java.util.Map;

/**
 * 消息删除请求绑定节点。
 * <p>
 * 解析删除消息接口请求，并写入频道 ID 与消息 ID。
 */
@Slf4j
@LiteflowComponent("ApiMessageDeleteBind")
public class ApiMessageDeleteBindNode extends CPNodeComponent {

    /**
     * 解析并绑定消息删除请求。
     *
     * <p>依赖上下文：{@link CPFlowKeys#REQUEST}（类型必须为 {@link MessageDeleteRequest}）。
     *
     * <p>输出：{@link CPNodeMessageKeys#MESSAGE_INFO_ID}。
     *
     * <p>失败语义：参数非法时抛出 {@link CPProblemException}（HTTP 422）。
     */
    @Override
    protected void process(CPFlowContext context) {
        Object reqObj = context.get(CPFlowKeys.REQUEST);
        if (!(reqObj instanceof MessageDeleteRequest req)) {
            throw new CPProblemException(CPProblem.of(CPProblemReason.VALIDATION_FAILED, "validation failed"));
        }
        long mid = parseId(req.mid(), "mid");
        context.set(CPNodeMessageKeys.MESSAGE_INFO_ID, mid);
        log.debug("消息删除绑定：完成：mid={}", mid);
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
}
