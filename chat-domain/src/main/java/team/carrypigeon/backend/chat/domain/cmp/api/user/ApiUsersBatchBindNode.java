package team.carrypigeon.backend.chat.domain.cmp.api.user;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeUserKeys;
import team.carrypigeon.backend.chat.domain.controller.web.api.dto.UsersBatchRequest;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Bind {@code GET /api/users?ids=...} into context keys.
 * <p>
 * Input: {@link CPFlowKeys#REQUEST} = {@link UsersBatchRequest}
 * Output: {@link CPNodeUserKeys#USER_INFO_ID_LIST}
 */
@Slf4j
@LiteflowComponent("ApiUsersBatchBind")
public class ApiUsersBatchBindNode extends CPNodeComponent {

    private static final int MAX_IDS = 200;

    @Override
    protected void process(CPFlowContext context) {
        Object reqObj = context.get(CPFlowKeys.REQUEST);
        if (!(reqObj instanceof UsersBatchRequest req)) {
            throw new CPProblemException(CPProblem.of(422, "validation_failed", "validation failed"));
        }
        List<String> raw = req.ids();
        if (raw == null || raw.isEmpty()) {
            context.set(CPNodeUserKeys.USER_INFO_ID_LIST, List.of());
            return;
        }
        if (raw.size() > MAX_IDS) {
            throw new CPProblemException(CPProblem.of(422, "validation_failed", "validation failed",
                    Map.of("field_errors", List.of(
                            Map.of("field", "ids", "reason", "too_many", "message", "too many ids")
                    ))));
        }
        Set<Long> uniqueOrdered = new LinkedHashSet<>();
        for (String s : raw) {
            if (s == null || s.isBlank()) {
                continue;
            }
            uniqueOrdered.add(parseId(s.trim()));
        }
        List<Long> ids = new ArrayList<>(uniqueOrdered);
        context.set(CPNodeUserKeys.USER_INFO_ID_LIST, ids);
        log.debug("ApiUsersBatchBind success, size={}", ids.size());
    }

    private long parseId(String str) {
        try {
            return Long.parseLong(str);
        } catch (Exception e) {
            throw new CPProblemException(CPProblem.of(422, "validation_failed", "validation failed",
                    Map.of("field_errors", List.of(
                            Map.of("field", "ids", "reason", "invalid", "message", "invalid id")
                    ))));
        }
    }
}

