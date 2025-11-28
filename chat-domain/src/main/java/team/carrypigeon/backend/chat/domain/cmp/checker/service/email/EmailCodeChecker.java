package team.carrypigeon.backend.chat.domain.cmp.checker.service.email;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.chat.domain.controller.CPNodeComponent;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.api.dao.cache.CPCache;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyBasicConstants;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyExtraConstants;
import team.carrypigeon.backend.chat.domain.cmp.info.CheckResult;

/**
 * ????????</br>
 * ???????????<br/>
 * ???Email:String;Email_Code:Long<br/>
 * ???
 * <ul>
 *     <li>??????????????????????</li>
 *     <li>??????bind type=soft???? CheckResult</li>
 * </ul>
 */
@Slf4j
@AllArgsConstructor
@LiteflowComponent("EmailCodeChecker")
public class EmailCodeChecker extends CPNodeComponent {

    private static final String BIND_TYPE_KEY = "type";

    private final CPCache cache;

    @Override
    protected void process(CPSession session, DefaultContext context) throws Exception {
        String type = getBindData(BIND_TYPE_KEY, String.class);
        boolean soft = "soft".equalsIgnoreCase(type);

        // ????
        String email = context.getData(CPNodeValueKeyExtraConstants.EMAIL);
        Long code = context.getData(CPNodeValueKeyExtraConstants.EMAIL_CODE);
        if (email == null || code == null) {
            // ?????????
            log.error("EmailCodeChecker param error: email or code is null");
            context.setData(CPNodeValueKeyBasicConstants.RESPONSE,
                    CPResponse.SERVER_ERROR.copy().setTextData("email code param error"));
            throw new CPReturnException();
        }
        String serverCode = cache.getAndDelete(email + ":code");
        if (serverCode == null || !serverCode.equals(code.longValue() + "")) {
            if (soft) {
                context.setData(CPNodeValueKeyBasicConstants.CHECK_RESULT,
                        new CheckResult(false, "email code error"));
                log.info("EmailCodeChecker soft fail: code error, email={}", email);
                return;
            }
            log.info("EmailCodeChecker hard fail: code error, email={}", email);
            context.setData(CPNodeValueKeyBasicConstants.RESPONSE,
                    CPResponse.ERROR_RESPONSE.copy().setTextData("email code error"));
            throw new CPReturnException();
        }
        if (soft) {
            context.setData(CPNodeValueKeyBasicConstants.CHECK_RESULT, new CheckResult(true, null));
            log.debug("EmailCodeChecker soft success, email={}", email);
        }
        // ???????????
    }
}
