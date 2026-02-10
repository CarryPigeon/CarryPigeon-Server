package team.carrypigeon.backend.chat.domain.cmp.biz.channel.application;

import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.application.CPChannelApplication;
import team.carrypigeon.backend.api.bo.domain.channel.application.CPChannelApplicationStateEnum;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemReason;
import team.carrypigeon.backend.api.chat.domain.flow.CheckResult;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelApplicationKeys;

/**
 * 频道申请状态设置组件（checker 风格）<br/>
 * 入参：<br/>
 * 1. ChannelApplicationInfo:{@link CPChannelApplication}<br/>
 * 2. ChannelApplicationInfo_State:Integer（1=APPROVED，2=REJECTED）<br/>
 * 出参：<br/>
 * 1. ChannelApplicationInfo.state 被更新为对应枚举值<br/>
 * 2. check_result:{@link CheckResult}，msg 为 "approved" 或 "rejected"，用于后续分支选择<br/>
 *
 * 使用方式：先执行本组件设置状态并写入 check_result，再通过<br/>
 * SWITCH(CheckerResultReader.bind("key","msg")) 根据 msg 分支到 approved/rejected。
 *
 * @author midreamsheep
 */
@Slf4j
@LiteflowComponent("ChannelApplicationStateSetterSwitcher")
public class CPChannelApplicationStateSetterSwitcherNode extends CPNodeComponent {

    /**
     * 执行当前节点的核心处理逻辑。
     *
     * @param session 当前请求会话（仅用于节点签名）
     * @param context LiteFlow 上下文，读取审批结果并更新申请状态
     * @throws Exception 执行过程中抛出的异常
     */
    @Override
    public void process(CPSession session, CPFlowContext context) throws Exception {
        CPChannelApplication channelApplicationInfo = requireContext(context, CPNodeChannelApplicationKeys.CHANNEL_APPLICATION_INFO);
        Integer state = requireContext(context, CPNodeChannelApplicationKeys.CHANNEL_APPLICATION_INFO_STATE);

        if (channelApplicationInfo.getState() != null && channelApplicationInfo.getState() != CPChannelApplicationStateEnum.PENDING) {
            fail(CPProblem.of(CPProblemReason.APPLICATION_ALREADY_PROCESSED, "application already processed"));
        }

        CPChannelApplicationStateEnum stateEnum = CPChannelApplicationStateEnum.valueOf(state);
        if (stateEnum == null || stateEnum == CPChannelApplicationStateEnum.PENDING) {
            log.error("ChannelApplicationStateSetterSwitcher args error: invalid state value={}", state);
            validationFailed();
            return;
        }
        channelApplicationInfo.setState(stateEnum);
        String tag = switch (stateEnum) {
            case APPROVED -> "approved";
            case REJECTED -> "rejected";
            default -> "unknown";
        };
        context.set(CPFlowKeys.CHECK_RESULT, new CheckResult(true, tag));
        log.debug("ChannelApplicationStateSetterSwitcher success, state={}, tag={}", stateEnum, tag);
    }
}
