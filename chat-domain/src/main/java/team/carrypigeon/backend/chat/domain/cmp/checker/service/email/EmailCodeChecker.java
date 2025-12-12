package team.carrypigeon.backend.chat.domain.cmp.checker.service.email;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.api.dao.cache.CPCache;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeBindKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeCommonKeys;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyExtraConstants;
import team.carrypigeon.backend.chat.domain.cmp.info.CheckResult;

/**
 * 邮箱验证码校验节点。</br>
 * 从缓存中读取并删除验证码，与客户端提交的验证码进行比对。<br/>
 * 输入：Email:String; Email_Code:Integer<br/>
 * 输出：<br/>
 * <ul>
 *     <li>hard 模式：验证码错误时返回错误响应</li>
 *     <li>soft 模式（bind type=soft）：仅写入 {@link CheckResult}</li>
 * </ul>
 */
@Slf4j
@AllArgsConstructor
@LiteflowComponent("EmailCodeChecker")
public class EmailCodeChecker extends CPNodeComponent {

    private static final String BIND_TYPE_KEY = CPNodeBindKeys.TYPE;

    private final CPCache cache;

    @Override
    public void process(CPSession session, DefaultContext context) throws Exception {
        String type = getBindData(BIND_TYPE_KEY, String.class);
        boolean soft = "soft".equalsIgnoreCase(type);

        // 读取参数
        String email = context.getData(CPNodeValueKeyExtraConstants.EMAIL);
        Integer code = context.getData(CPNodeValueKeyExtraConstants.EMAIL_CODE);
        if (email == null || code == null) {
            // 参数缺失，直接返回服务器错误
            log.error("EmailCodeChecker param error: email or code is null");
            context.setData(CPNodeCommonKeys.RESPONSE,
                    CPResponse.SERVER_ERROR.copy().setTextData("email code param error"));
            throw new CPReturnException();
        }
        String serverCode = cache.getAndDelete(email + ":code");
        if (serverCode == null || !serverCode.equals(code.longValue() + "")) {
            if (soft) {
                context.setData(CPNodeCommonKeys.CHECK_RESULT,
                        new CheckResult(false, "email code error"));
                log.info("EmailCodeChecker soft fail: code error, email={}", email);
                return;
            }
            log.info("EmailCodeChecker hard fail: code error, email={}", email);
            context.setData(CPNodeCommonKeys.RESPONSE,
                    CPResponse.ERROR_RESPONSE.copy().setTextData("email code error"));
            throw new CPReturnException();
        }
        if (soft) {
            context.setData(CPNodeCommonKeys.CHECK_RESULT, new CheckResult(true, null));
            log.debug("EmailCodeChecker soft success, email={}", email);
        }
        // 校验通过，这里无需额外处理
    }
}
