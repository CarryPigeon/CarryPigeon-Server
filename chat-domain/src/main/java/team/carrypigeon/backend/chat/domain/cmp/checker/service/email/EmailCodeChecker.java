package team.carrypigeon.backend.chat.domain.cmp.checker.service.email;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.AbstractCheckerNode;
import team.carrypigeon.backend.api.dao.cache.CPCache;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyExtraConstants;

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
public class EmailCodeChecker extends AbstractCheckerNode {

    private final CPCache cache;

    @Override
    public void process(CPSession session, CPFlowContext context) throws Exception {
        boolean soft = isSoftMode();

        // 读取参数
        String email = requireContext(context, CPNodeValueKeyExtraConstants.EMAIL);
        Integer code = requireContext(context, CPNodeValueKeyExtraConstants.EMAIL_CODE);

        String serverCode = cache.getAndDelete(email + ":code");
        if (serverCode == null || !serverCode.equals(code.longValue() + "")) {
            if (soft) {
                markSoftFail(context, "email code error");
                log.info("EmailCodeChecker soft fail: code error, email={}", email);
                return;
            }
            log.info("EmailCodeChecker hard fail: code error, email={}", email);
            fail(CPProblem.of(422, "email_code_invalid", "email code error"));
        }
        if (soft) {
            markSoftSuccess(context);
            log.debug("EmailCodeChecker soft success, email={}", email);
        }
        // 校验通过，这里无需额外处理
    }
}
