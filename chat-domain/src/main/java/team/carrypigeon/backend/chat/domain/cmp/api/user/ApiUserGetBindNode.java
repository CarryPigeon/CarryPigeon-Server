package team.carrypigeon.backend.chat.domain.cmp.api.user;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemException;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemReason;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeUserKeys;
import team.carrypigeon.backend.chat.domain.controller.web.api.dto.UserIdRequest;

import java.util.List;
import java.util.Map;

/**
 * 用户详情查询绑定节点。
 * <p>
 * 解析 `GET /api/users/{uid}` 路由参数并写入用户上下文键。
 */
@Slf4j
@LiteflowComponent("ApiUserGetBind")
public class ApiUserGetBindNode extends CPNodeComponent {

    /**
     * 解析并绑定用户 ID。
     */
    @Override
    protected void process(CPFlowContext context) {
        Object reqObj = context.get(CPFlowKeys.REQUEST);
        if (!(reqObj instanceof UserIdRequest req)) {
            throw new CPProblemException(CPProblem.of(CPProblemReason.VALIDATION_FAILED, "validation failed"));
        }
        long uid = parseId(req.uid(), "uid");
        context.set(CPNodeUserKeys.USER_INFO_ID, uid);
        log.debug("ApiUserGetBind success, uid={}", uid);
    }

    /**
     * 解析字符串形式的用户 ID。
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
