package team.carrypigeon.backend.chat.domain.cmp.api.channel.application;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemException;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemReason;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelApplicationKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;
import team.carrypigeon.backend.chat.domain.controller.web.api.dto.ChannelApplicationDecisionInternalRequest;

import java.util.List;
import java.util.Map;

/**
 * 入群申请审批请求绑定节点。
 * <p>
 * 解析 `POST /api/channels/{cid}/applications/{application_id}/decisions` 请求并写入上下文。
 */
@Slf4j
@LiteflowComponent("ApiChannelApplicationDecisionBind")
public class ApiChannelApplicationDecisionBindNode extends CPNodeComponent {

    /**
     * 解析并绑定申请审批参数。
     */
    @Override
    protected void process(CPFlowContext context) {
        Object reqObj = context.get(CPFlowKeys.REQUEST);
        if (!(reqObj instanceof ChannelApplicationDecisionInternalRequest req) || req.body() == null) {
            throw new CPProblemException(CPProblem.of(CPProblemReason.VALIDATION_FAILED, "validation failed"));
        }

        long cid = parseId(req.cid(), "cid");
        long applicationId = parseId(req.applicationId(), "application_id");
        String decision = req.body().decision();

        Integer state = switch (decision) {
            case "approve" -> 1;
            case "reject" -> 2;
            default -> null;
        };
        if (state == null) {
            throw new CPProblemException(CPProblem.of(CPProblemReason.VALIDATION_FAILED, "validation failed",
                    Map.of("field_errors", List.of(
                            Map.of("field", "decision", "reason", "invalid", "message", "decision must be approve/reject")
                    ))));
        }

        context.set(CPNodeChannelKeys.CHANNEL_INFO_ID, cid);
        context.set(CPNodeChannelApplicationKeys.CHANNEL_APPLICATION_INFO_ID, applicationId);
        context.set(CPNodeChannelApplicationKeys.CHANNEL_APPLICATION_INFO_STATE, state);
        log.debug("ApiChannelApplicationDecisionBind success, cid={}, applicationId={}, decision={}", cid, applicationId, decision);
    }

    /**
     * 解析字符串形式的 ID。
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
