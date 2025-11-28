package team.carrypigeon.backend.chat.domain.cmp.checker.user;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.chat.domain.controller.CPNodeComponent;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.chat.domain.attribute.CPChatDomainAttributes;
import team.carrypigeon.backend.chat.domain.cmp.info.CheckResult;

/**
 * 判断用户是否登录的校验器<br/>
 * 入参：无<br/>
 * 出参：
 * <ul>
 *     <li>硬失败模式（默认）：SessionId:Long</li>
 *     <li>软失败模式（bind type=soft）：SessionId:Long（成功时），CheckResult</li>
 * </ul>
 * type bind: hard|soft, 默认 hard。
 * @author midreamsheep
 */
@Slf4j
@LiteflowComponent("UserLoginChecker")
public class UserLoginCheckerNode extends CPNodeComponent {

    private static final String BIND_TYPE_KEY = "type";

    @Override
    protected void process(CPSession session, DefaultContext context) throws Exception {
        String type = getBindData(BIND_TYPE_KEY, String.class);
        boolean soft = "soft".equalsIgnoreCase(type);

        Long attributeValue = session.getAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, Long.class);
        if (attributeValue == null) {
            if (soft) {
                // 软失败：仅写入 CheckResult，不中断流程
                context.setData("CheckResult", new CheckResult(false, "user not login"));
                log.info("UserLoginChecker soft fail: user not login");
                return;
            }
            // 硬失败：直接返回未登录错误
            log.info("UserLoginChecker hard fail: user not login");
            context.setData("response",
                    CPResponse.AUTHORITY_ERROR_RESPONSE.copy().setTextData("user not login"));
            throw new CPReturnException();
        }
        // 登录成功，写入 SessionId
        context.setData("SessionId", attributeValue);
        if (soft) {
            context.setData("CheckResult", new CheckResult(true, null));
            log.debug("UserLoginChecker soft success, uid={}", attributeValue);
        }
    }
}
