package team.carrypigeon.backend.chat.domain.cmp.checker.service.email;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.chat.domain.controller.CPNodeComponent;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyBasicConstants;
import team.carrypigeon.backend.chat.domain.cmp.info.CheckResult;

/**
 * ??????<br/>
 * ??????????<br/>
 * ???Email:String<br/>
 * ???
 * <ul>
 *     <li>?????????????????????</li>
 *     <li>??????bind type=soft???? CheckResult</li>
 * </ul>
 */
@Slf4j
@LiteflowComponent("EmailValidChecker")
public class EmailValidChecker extends CPNodeComponent {

    private static final String EMAIL_VALID_CHECKER_PARAM = "Email";
    private static final String BIND_TYPE_KEY = "type";

    @Override
    protected void process(CPSession session, DefaultContext context) throws Exception {
        String type = getBindData(BIND_TYPE_KEY, String.class);
        boolean soft = "soft".equalsIgnoreCase(type);

        // ????????
        String email = context.getData(EMAIL_VALID_CHECKER_PARAM);
        if (email == null) {
            // ????????????????
            log.error("EmailValidChecker args error: Email is null");
            argsError(context);
            return;
        }
        if (!email.matches("^[a-zA-Z0-9_+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+$")) {
            if (soft) {
                context.setData(CPNodeValueKeyBasicConstants.CHECK_RESULT,
                        new CheckResult(false, "email error"));
                log.info("EmailValidChecker soft fail: invalid email format, email={}", email);
                return;
            }
            log.info("EmailValidChecker hard fail: invalid email format, email={}", email);
            context.setData(CPNodeValueKeyBasicConstants.RESPONSE,
                    CPResponse.ERROR_RESPONSE.copy().setTextData("email error"));
            throw new CPReturnException();
        }
        if (soft) {
            context.setData(CPNodeValueKeyBasicConstants.CHECK_RESULT, new CheckResult(true, null));
            log.debug("EmailValidChecker soft success, email={}", email);
        }
    }
}
