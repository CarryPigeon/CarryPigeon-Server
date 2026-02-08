package team.carrypigeon.backend.chat.domain.cmp.checker.group;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMemberAuthorityEnum;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.AbstractCheckerNode;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelMemberKeys;

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
public class CPChannelBanTargetCheckerNode extends AbstractCheckerNode {

    @Override
    public void process(CPSession session, CPFlowContext context) throws Exception {
        boolean soft = isSoftMode();

        // 必填参数：ChannelMemberInfo
        CPChannelMember target = requireContext(context, CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO);
        if (target.getAuthority() == CPChannelMemberAuthorityEnum.ADMIN) {
            if (soft) {
                markSoftFail(context, "target is admin");
                log.info("CPChannelBanTargetChecker soft fail: target is admin, uid={}, cid={}",
                        target.getUid(), target.getCid());
                return;
            }
            log.info("CPChannelBanTargetChecker hard fail: target is admin, uid={}, cid={}",
                    target.getUid(), target.getCid());
            forbidden("cannot_ban_admin", "cannot ban channel admin");
        }
        if (soft) {
            markSoftSuccess(context);
            log.debug("CPChannelBanTargetChecker soft success, uid={}, cid={}",
                    target.getUid(), target.getCid());
        }
    }
}
