package team.carrypigeon.backend.chat.domain.cmp.api;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemReason;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeUserKeys;

/**
 * API 登录守卫节点。
 * <p>
 * 用于校验控制层注入的认证用户，并把用户 ID 写入链路通用上下文键。
 * <p>
 * 入参：{@link CPFlowKeys#AUTH_UID}。<br>
 * 出参：{@link CPFlowKeys#SESSION_UID}、{@link CPNodeUserKeys#USER_INFO_ID}。<br>
 * 失败：抛出 401 {@code unauthorized}。
 */
@Slf4j
@LiteflowComponent("ApiLoginGuard")
public class ApiLoginGuardNode extends CPNodeComponent {

    /**
     * 校验登录态并写入会话用户 ID。
     */
    @Override
    protected void process(CPFlowContext context) {
        Long uid = requireContext(context, CPFlowKeys.AUTH_UID);
        if (uid <= 0) {
            fail(CPProblem.of(CPProblemReason.UNAUTHORIZED, "missing or invalid access token"));
        }
        context.set(CPFlowKeys.SESSION_UID, uid);
        context.set(CPNodeUserKeys.USER_INFO_ID, uid);
    }
}
