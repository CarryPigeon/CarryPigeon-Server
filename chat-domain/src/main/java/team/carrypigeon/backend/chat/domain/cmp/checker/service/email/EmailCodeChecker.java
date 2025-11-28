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
import team.carrypigeon.backend.chat.domain.cmp.info.CheckResult;

/**
 * 邮箱验证码检查器</br>
 * 验证邮箱验证码是否正确<br/>
 * 入参：Email:String;Email_Code:Long<br/>
 * 出参：
 * <ul>
 *     <li>硬失败模式（默认）：验证码错误时直接返回错误</li>
 *     <li>软失败模式（bind type=soft）：写入 CheckResult</li>
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

        // 获取参数
        String email = context.getData("Email");
        Long code = context.getData("Email_Code");
        if (email == null || code == null) {
            // 参数错误视为硬失败
            log.error("EmailCodeChecker param error: email or code is null");
            context.setData("response",
                    CPResponse.SERVER_ERROR.copy().setTextData("email code param error"));
            throw new CPReturnException();
        }
        String serverCode = cache.getAndDelete(email + ":code");
        if (serverCode == null || !serverCode.equals(code.longValue() + "")) {
            if (soft) {
                context.setData("CheckResult", new CheckResult(false, "email code error"));
                log.info("EmailCodeChecker soft fail: code error, email={}", email);
                return;
            }
            log.info("EmailCodeChecker hard fail: code error, email={}", email);
            context.setData("response",
                    CPResponse.ERROR_RESPONSE.copy().setTextData("email code error"));
            throw new CPReturnException();
        }
        if (soft) {
            context.setData("CheckResult", new CheckResult(true, null));
            log.debug("EmailCodeChecker soft success, email={}", email);
        }
        // 成功执行则不做额外处理
    }
}
