package team.carrypigeon.backend.chat.domain.cmp.api;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeUserKeys;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;

/**
 * Login guard for HTTP `/api` chains.
 * <p>
 * This node validates authentication and writes uid into common keys used by domain nodes.
 * <p>
 * Input:
 * <ul>
 *   <li>{@link CPFlowKeys#AUTH_UID}: {@code Long} (set by controllers)</li>
 * </ul>
 * Output:
 * <ul>
 *   <li>{@link CPFlowKeys#SESSION_UID}: {@code Long} current user id</li>
 *   <li>{@link CPNodeUserKeys#USER_INFO_ID}: {@code Long} current user id (user domain convenience)</li>
 * </ul>
 * Failure:
 * throws {@link CPProblemException} with {@code 401 unauthorized}.
 */
@Slf4j
@LiteflowComponent("ApiLoginGuard")
public class ApiLoginGuardNode extends CPNodeComponent {

    @Override
    protected void process(CPFlowContext context) {
        Long uid = requireContext(context, CPFlowKeys.AUTH_UID);
        if (uid <= 0) {
            fail(CPProblem.of(401, "unauthorized", "missing or invalid access token"));
        }
        context.set(CPFlowKeys.SESSION_UID, uid);
        context.set(CPNodeUserKeys.USER_INFO_ID, uid);
    }
}
