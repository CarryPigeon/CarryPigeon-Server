package team.carrypigeon.backend.chat.domain.cmp.api.channel.application;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelMemberKeys;
import team.carrypigeon.backend.chat.domain.controller.web.api.dto.ChannelApplicationCreateInternalRequest;

import java.util.List;
import java.util.Map;

/**
 * Bind {@code POST /api/channels/{cid}/applications}.
 */
@Slf4j
@LiteflowComponent("ApiChannelApplicationCreateBind")
public class ApiChannelApplicationCreateBindNode extends CPNodeComponent {

    @Override
    protected void process(CPFlowContext context) {
        Object reqObj = context.get(CPFlowKeys.REQUEST);
        if (!(reqObj instanceof ChannelApplicationCreateInternalRequest req) || req.body() == null) {
            throw new CPProblemException(CPProblem.of(422, "validation_failed", "validation failed"));
        }
        long cid = parseId(req.cid(), "cid");
        String reason = req.body().reason();
        if (reason == null || reason.isBlank()) {
            throw new CPProblemException(CPProblem.of(422, "validation_failed", "validation failed",
                    Map.of("field_errors", List.of(
                            Map.of("field", "reason", "reason", "invalid", "message", "reason is required")
                    ))));
        }
        context.set(CPNodeChannelKeys.CHANNEL_INFO_ID, cid);
        context.set(CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO_MSG, reason);
        log.debug("ApiChannelApplicationCreateBind success, cid={}", cid);
    }

    private long parseId(String str, String field) {
        try {
            return Long.parseLong(str);
        } catch (Exception e) {
            throw new CPProblemException(CPProblem.of(422, "validation_failed", "validation failed",
                    Map.of("field_errors", List.of(
                            Map.of("field", field, "reason", "invalid", "message", "invalid id")
                    ))));
        }
    }
}

