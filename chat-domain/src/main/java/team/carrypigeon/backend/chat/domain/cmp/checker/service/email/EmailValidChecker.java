package team.carrypigeon.backend.chat.domain.cmp.checker.service.email;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.AbstractCheckerNode;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeBindKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeCommonKeys;
import team.carrypigeon.backend.chat.domain.cmp.info.CheckResult;

/**
 * 邮箱格式合法性校验节点。<br/>
 * 使用正则表达式校验邮箱字符串是否符合基本格式。<br/>
 * 输入：Email:String<br/>
 * 输出：<br/>
 * <ul>
 *     <li>hard 模式：格式非法时直接返回错误响应</li>
 *     <li>soft 模式（bind type=soft）：仅将结果写入 {@link CheckResult}</li>
 * </ul>
 */
@Slf4j
@LiteflowComponent("EmailValidChecker")
public class EmailValidChecker extends AbstractCheckerNode {

    private static final String EMAIL_VALID_CHECKER_PARAM = "Email";
    private static final String BIND_TYPE_KEY = CPNodeBindKeys.TYPE;

    @Override
    public void process(CPSession session, CPFlowContext context) throws Exception {
        boolean soft = isSoftMode();

        // 读取邮箱参数（必填）
        String email = requireContext(context, EMAIL_VALID_CHECKER_PARAM, String.class);
        if (!email.matches("^[a-zA-Z0-9_+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+$")) {
            if (soft) {
                markSoftFail(context, "email error");
                log.info("EmailValidChecker soft fail: invalid email format, email={}", email);
                return;
            }
            log.info("EmailValidChecker hard fail: invalid email format, email={}", email);
            context.setData(CPNodeCommonKeys.RESPONSE,
                    CPResponse.error("email error"));
            throw new CPReturnException();
        }
        if (soft) {
            markSoftSuccess(context);
            log.debug("EmailValidChecker soft success, email={}", email);
        }
    }
}
