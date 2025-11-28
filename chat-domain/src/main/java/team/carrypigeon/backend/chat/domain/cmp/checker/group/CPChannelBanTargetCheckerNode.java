package team.carrypigeon.backend.chat.domain.cmp.checker.group;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMemberAuthorityEnum;
import team.carrypigeon.backend.api.chat.domain.controller.CPNodeComponent;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyBasicConstants;
import team.carrypigeon.backend.chat.domain.cmp.info.CheckResult;

/**
 * ???????????????
 * ?????????????????????<br/>
 * ???ChannelMemberInfo:{@link CPChannelMember}<br/>
 * ???
 * <ul>
 *     <li>???????????????????????</li>
 *     <li>??????bind type=soft???? CheckResult</li>
 * </ul>
 */
@Slf4j
@LiteflowComponent("CPChannelBanTargetChecker")
public class CPChannelBanTargetCheckerNode extends CPNodeComponent {

    private static final String BIND_TYPE_KEY = "type";

    @Override
    protected void process(CPSession session, DefaultContext context) throws Exception {
        String type = getBindData(BIND_TYPE_KEY, String.class);
        boolean soft = "soft".equalsIgnoreCase(type);

        CPChannelMember target = context.getData(CPNodeValueKeyBasicConstants.CHANNEL_MEMBER_INFO);
        if (target == null) {
            log.error("CPChannelBanTargetChecker args error: ChannelMemberInfo is null");
            argsError(context);
            return;
        }
        if (target.getAuthority() == CPChannelMemberAuthorityEnum.ADMIN) {
            if (soft) {
                context.setData(CPNodeValueKeyBasicConstants.CHECK_RESULT,
                        new CheckResult(false, "target is admin"));
                log.info("CPChannelBanTargetChecker soft fail: target is admin, uid={}, cid={}",
                        target.getUid(), target.getCid());
                return;
            }
            log.info("CPChannelBanTargetChecker hard fail: target is admin, uid={}, cid={}",
                    target.getUid(), target.getCid());
            context.setData(CPNodeValueKeyBasicConstants.RESPONSE,
                    CPResponse.ERROR_RESPONSE.copy().setTextData("cannot ban channel admin"));
            throw new CPReturnException();
        }
        if (soft) {
            context.setData(CPNodeValueKeyBasicConstants.CHECK_RESULT, new CheckResult(true, null));
            log.debug("CPChannelBanTargetChecker soft success, uid={}, cid={}",
                    target.getUid(), target.getCid());
        }
    }
}
