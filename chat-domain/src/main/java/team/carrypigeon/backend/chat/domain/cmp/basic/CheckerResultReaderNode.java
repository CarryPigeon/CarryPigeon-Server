package team.carrypigeon.backend.chat.domain.cmp.basic;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.chat.domain.controller.CPNodeSwitchComponent;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyBasicConstants;
import team.carrypigeon.backend.chat.domain.cmp.info.CheckResult;

/**
 * 读取 {@link CheckResult} 的节点，用于将检查结果转换为 LiteFlow 分支标签。<br/>
 * <p>
 * bind 配置：<br/>
 * - key=state（默认）：根据 CheckResult.state 返回 "success" 或 "fail" 标签；<br/>
 * - key=msg：返回 CheckResult.msg 作为标签。<br/>
 * 入参：CheckResult:{@link CheckResult}<br/>
 * 出参：无（返回值作为 LiteFlow 分支标签使用）<br/>
 */
@Slf4j
@LiteflowComponent("CheckerResultReader")
public class CheckerResultReaderNode extends CPNodeSwitchComponent {

    private static final String BIND_KEY = "key";
    private static final String MODE_STATE = "state";
    private static final String MODE_MSG = "msg";

    @Override
    protected String process(CPSession session, DefaultContext context) throws Exception {
        CheckResult result = context.getData(CPNodeValueKeyBasicConstants.CHECK_RESULT);
        if (result == null) {
            log.error("CheckerResultReader: CheckResult is null in context");
            argsError(context);
            // argsError 会抛出异常，这里的返回值不会被使用
            return null;
        }
        String mode = getBindData(BIND_KEY, String.class);
        if (mode == null) {
            mode = MODE_STATE;
        }
        switch (mode) {
            case MODE_STATE:
                // 根据 state 返回 success 或 fail
                String tag = result.state() ? "success" : "fail";
                log.debug("CheckerResultReader: mode=state, state={}, tag={}", result.state(), tag);
                return tag;
            case MODE_MSG:
                // 直接返回自定义 msg 作为标签
                String msg = result.msg();
                if (msg == null || msg.isEmpty()) {
                    // msg 为空时，退化为 state 模式
                    String fallback = result.state() ? "success" : "fail";
                    log.debug("CheckerResultReader: mode=msg but msg is empty, fallback tag={}", fallback);
                    return fallback;
                }
                log.debug("CheckerResultReader: mode=msg, tag={}", msg);
                return msg;
            default:
                log.error("CheckerResultReader: unsupported mode '{}'", mode);
                argsError(context);
                return null;
        }
    }
}
