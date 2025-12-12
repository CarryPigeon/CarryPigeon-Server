package team.carrypigeon.backend.chat.domain.cmp.checker.user;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.chat.domain.attribute.CPChatDomainAttributes;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeBindKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeCommonKeys;
import team.carrypigeon.backend.chat.domain.cmp.info.CheckResult;

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
public class UserLoginCheckerNode extends CPNodeComponent {

    private static final String BIND_TYPE_KEY = CPNodeBindKeys.TYPE;

    @Override
    public void process(CPSession session, DefaultContext context) throws Exception {
        String type = getBindData(BIND_TYPE_KEY, String.class);
        boolean soft = "soft".equalsIgnoreCase(type);

        Long attributeValue = session.getAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, Long.class);
        if (attributeValue == null) {
            if (soft) {
                // 未登录：soft 模式下仅写入 CheckResult，不中断流程
                context.setData(CPNodeCommonKeys.CHECK_RESULT,
                        new CheckResult(false, "user not login"));
                log.info("UserLoginChecker soft fail: user not login");
                return;
            }
            // 未登录：hard 模式下直接返回权限错误
            log.info("UserLoginChecker hard fail: user not login");
            context.setData(CPNodeCommonKeys.RESPONSE,
                    CPResponse.AUTHORITY_ERROR_RESPONSE.copy().setTextData("user not login"));
            throw new CPReturnException();
        }
        // 写入 SessionId，供后续节点使用
        context.setData(CPNodeCommonKeys.SESSION_ID, attributeValue);
        if (soft) {
            context.setData(CPNodeCommonKeys.CHECK_RESULT, new CheckResult(true, null));
            log.debug("UserLoginChecker soft success, uid={}", attributeValue);
        }
    }
}
