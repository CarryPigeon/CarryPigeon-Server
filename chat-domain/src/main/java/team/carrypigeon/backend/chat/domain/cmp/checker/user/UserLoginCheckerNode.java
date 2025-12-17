package team.carrypigeon.backend.chat.domain.cmp.checker.user;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.AbstractCheckerNode;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.chat.domain.attribute.CPChatDomainAttributes;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeCommonKeys;

/**
 * 会话登录状态检查组件。<br/>
 * 从会话属性中读取当前登录用户 id。<br/>
 * 输入：<br/>
 *  - 无（从 session 属性中读取）<br/>
 * 输出：<br/>
 * <ul>
 *     <li>SessionId:Long 当前登录用户 id</li>
 *     <li>soft 模式（bind type=soft）下，还会写入 {@link CheckResult}</li>
 * </ul>
 * type bind: hard|soft，默认 hard。
 * @author midreamsheep
 */
@Slf4j
@LiteflowComponent("UserLoginChecker")
public class UserLoginCheckerNode extends AbstractCheckerNode {

    @Override
    public void process(CPSession session, CPFlowContext context) throws Exception {
        boolean soft = isSoftMode();

        Long attributeValue = session.getAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, Long.class);
        if (attributeValue == null) {
            if (soft) {
                // 未登录：soft 模式下仅写入 CheckResult，不中断流程
                markSoftFail(context, "user not login");
                log.info("UserLoginChecker soft fail: user not login");
                return;
            }
            // 未登录：hard 模式下直接返回权限错误
            log.info("UserLoginChecker hard fail: user not login");
            context.setData(CPNodeCommonKeys.RESPONSE,
                    CPResponse.authorityError("user not login"));
            throw new CPReturnException();
        }
        // 写入 SessionId，供后续节点使用
        context.setData(CPNodeCommonKeys.SESSION_ID, attributeValue);
        if (soft) {
            markSoftSuccess(context);
            log.debug("UserLoginChecker soft success, uid={}", attributeValue);
        }
    }
}
