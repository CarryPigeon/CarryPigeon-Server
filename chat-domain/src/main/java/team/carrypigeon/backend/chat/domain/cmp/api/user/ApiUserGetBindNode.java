package team.carrypigeon.backend.chat.domain.cmp.api.user;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeUserKeys;
import team.carrypigeon.backend.chat.domain.controller.web.api.dto.UserIdRequest;

import java.util.List;
import java.util.Map;

/**
 * Bind {@code GET /api/users/{uid}} into existing context keys.
 * <p>
 * Input: {@link CPFlowKeys#REQUEST} = {@link UserIdRequest}
 * Output: {@link CPNodeUserKeys#USER_INFO_ID}
 */
@Slf4j
@LiteflowComponent("ApiUserGetBind")
public class ApiUserGetBindNode extends CPNodeComponent {

    @Override
    protected void process(CPFlowContext context) {
        Object reqObj = context.get(CPFlowKeys.REQUEST);
        if (!(reqObj instanceof UserIdRequest req)) {
            throw new CPProblemException(CPProblem.of(422, "validation_failed", "validation failed"));
        }
        long uid = parseId(req.uid(), "uid");
        context.set(CPNodeUserKeys.USER_INFO_ID, uid);
        log.debug("ApiUserGetBind success, uid={}", uid);
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

