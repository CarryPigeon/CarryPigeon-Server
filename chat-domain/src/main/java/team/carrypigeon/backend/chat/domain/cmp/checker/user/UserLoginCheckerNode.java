package team.carrypigeon.backend.chat.domain.cmp.checker.user;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.chat.domain.controller.CPNodeComponent;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.chat.domain.attribute.CPChatDomainAttributes;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyBasicConstants;
import team.carrypigeon.backend.chat.domain.cmp.info.CheckResult;

/**
 * ????????????<br/>
 * ????<br/>
 * ???
 * <ul>
 *     <li>??????????SessionId:Long</li>
 *     <li>??????bind type=soft??SessionId:Long??????CheckResult</li>
 * </ul>
 * type bind: hard|soft, ?? hard?
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
                // ??????? CheckResult??????
                context.setData(CPNodeValueKeyBasicConstants.CHECK_RESULT,
                        new CheckResult(false, "user not login"));
                log.info("UserLoginChecker soft fail: user not login");
                return;
            }
            // ?????????????
            log.info("UserLoginChecker hard fail: user not login");
            context.setData(CPNodeValueKeyBasicConstants.RESPONSE,
                    CPResponse.AUTHORITY_ERROR_RESPONSE.copy().setTextData("user not login"));
            throw new CPReturnException();
        }
        // ??????? SessionId
        context.setData(CPNodeValueKeyBasicConstants.SESSION_ID, attributeValue);
        if (soft) {
            context.setData(CPNodeValueKeyBasicConstants.CHECK_RESULT, new CheckResult(true, null));
            log.debug("UserLoginChecker soft success, uid={}", attributeValue);
        }
    }
}
