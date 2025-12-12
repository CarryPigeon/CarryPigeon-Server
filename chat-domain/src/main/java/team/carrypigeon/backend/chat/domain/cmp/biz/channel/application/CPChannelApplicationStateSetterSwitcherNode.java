package team.carrypigeon.backend.chat.domain.cmp.biz.channel.application;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.application.CPChannelApplication;
import team.carrypigeon.backend.api.bo.domain.channel.application.CPChannelApplicationStateEnum;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelApplicationKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeCommonKeys;
import team.carrypigeon.backend.chat.domain.cmp.info.CheckResult;

/**
 * 频道申请状态设置组件（checker 风格）<br/>
 * 入参：<br/>
 * 1. ChannelApplicationInfo:{@link CPChannelApplication}<br/>
 * 2. ChannelApplicationInfo_State:Integer（1=APPROVED，2=REJECTED）<br/>
 * 出参：<br/>
 * 1. ChannelApplicationInfo.state 被更新为对应枚举值<br/>
 * 2. CheckResult:{@link CheckResult}，msg 为 "approved" 或 "rejected"，用于后续分支选择<br/>
 *
 * 使用方式：先执行本组件设置状态并写入 CheckResult，再通过<br/>
 * SWITCH(CheckerResultReader.bind("key","msg")) 根据 msg 分支到 approved/rejected。
 *
 * @author midreamsheep
 */
@Slf4j
@LiteflowComponent("ChannelApplicationStateSetterSwitcher")
public class CPChannelApplicationStateSetterSwitcherNode extends CPNodeComponent {

    @Override
    public void process(CPSession session, DefaultContext context) throws Exception {
        CPChannelApplication channelApplicationInfo =
                context.getData(CPNodeChannelApplicationKeys.CHANNEL_APPLICATION_INFO);
        Integer state = context.getData(CPNodeChannelApplicationKeys.CHANNEL_APPLICATION_INFO_STATE);
        if (channelApplicationInfo == null || state == null) {
            log.error("ChannelApplicationStateSetterSwitcher args error: ChannelApplicationInfo or state is null");
            argsError(context);
            return;
        }

        CPChannelApplicationStateEnum stateEnum = CPChannelApplicationStateEnum.valueOf(state);
        if (stateEnum == null || stateEnum == CPChannelApplicationStateEnum.PENDING) {
            log.error("ChannelApplicationStateSetterSwitcher args error: invalid state value={}", state);
            argsError(context);
            return;
        }

        // 更新申请状态
        channelApplicationInfo.setState(stateEnum);

        // 根据状态写入 CheckResult，msg 用于后续 CheckerResultReader 分支
        String tag = switch (stateEnum) {
            case APPROVED -> "approved";
            case REJECTED -> "rejected";
            default -> "unknown";
        };
        context.setData(
                CPNodeCommonKeys.CHECK_RESULT,
                new CheckResult(true, tag)
        );
        log.debug("ChannelApplicationStateSetterSwitcher success, state={}, tag={}", stateEnum, tag);
    }
}
