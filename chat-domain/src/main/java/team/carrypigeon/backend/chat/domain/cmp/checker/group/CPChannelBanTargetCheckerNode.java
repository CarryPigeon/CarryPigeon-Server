package team.carrypigeon.backend.chat.domain.cmp.checker.group;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMemberAuthorityEnum;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeBindKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelMemberKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeCommonKeys;
import team.carrypigeon.backend.chat.domain.cmp.info.CheckResult;

/**
 * 校验封禁目标是否合法的节点。<br/>
 * 用于阻止对频道管理员执行封禁操作。<br/>
 * 输入：ChannelMemberInfo:{@link CPChannelMember}<br/>
 * 输出：<br/>
 * <ul>
 *     <li>hard 模式：当目标为管理员时直接返回错误</li>
 *     <li>soft 模式（bind type=soft）：仅将结果写入 {@link CheckResult}</li>
 * </ul>
 */
@Slf4j
@LiteflowComponent("CPChannelBanTargetChecker")
public class CPChannelBanTargetCheckerNode extends CPNodeComponent {

    private static final String BIND_TYPE_KEY = CPNodeBindKeys.TYPE;

    @Override
    public void process(CPSession session, DefaultContext context) throws Exception {
        String type = getBindData(BIND_TYPE_KEY, String.class);
        boolean soft = "soft".equalsIgnoreCase(type);

        CPChannelMember target = context.getData(CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO);
        if (target == null) {
            log.error("CPChannelBanTargetChecker args error: ChannelMemberInfo is null");
            argsError(context);
            return;
        }
        if (target.getAuthority() == CPChannelMemberAuthorityEnum.ADMIN) {
            if (soft) {
                context.setData(CPNodeCommonKeys.CHECK_RESULT,
                        new CheckResult(false, "target is admin"));
                log.info("CPChannelBanTargetChecker soft fail: target is admin, uid={}, cid={}",
                        target.getUid(), target.getCid());
                return;
            }
            log.info("CPChannelBanTargetChecker hard fail: target is admin, uid={}, cid={}",
                    target.getUid(), target.getCid());
            context.setData(CPNodeCommonKeys.RESPONSE,
                    CPResponse.ERROR_RESPONSE.copy().setTextData("cannot ban channel admin"));
            throw new CPReturnException();
        }
        if (soft) {
            context.setData(CPNodeCommonKeys.CHECK_RESULT, new CheckResult(true, null));
            log.debug("CPChannelBanTargetChecker soft success, uid={}, cid={}",
                    target.getUid(), target.getCid());
        }
    }
}
