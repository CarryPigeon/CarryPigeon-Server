package team.carrypigeon.backend.chat.domain.cmp.checker.group;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.domain.channel.ban.CPChannelBan;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.AbstractCheckerNode;
import team.carrypigeon.backend.api.dao.database.channel.ban.ChannelBanDAO;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelMemberKeys;

/**
 * 检查当前用户在频道内是否处于封禁状态。<br/>
 * 输入：ChannelInfo_Id:Long; ChannelMemberInfo_Uid:Long<br/>
 * 输出：<br/>
 * <ul>
 *     <li>存在有效封禁：hard 模式直接返回错误，soft 模式在 {@link CheckResult} 中标记失败</li>
 *     <li>封禁已过期：自动删除封禁记录，并视为校验通过</li>
 * </ul>
 */
@Slf4j
@AllArgsConstructor
@LiteflowComponent("CPChannelBanChecker")
public class CPChannelBanCheckerNode extends AbstractCheckerNode {

    private final ChannelBanDAO channelBanDAO;

    @Override
    protected void process(CPFlowContext context) throws Exception {
        boolean soft = isSoftMode();

        Long cid = requireContext(context, CPNodeChannelKeys.CHANNEL_INFO_ID);
        Long uid = requireContext(context, CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO_UID);
        CPChannelBan ban = select(context,
                buildSelectKey("channel_ban", java.util.Map.of("cid", cid, "uid", uid)),
                () -> channelBanDAO.getByChannelIdAndUserId(uid, cid));
        if (ban == null) {
            // 用户未被封禁，直接通过
            if (soft) {
                markSoftSuccess(context);
            }
            log.debug("CPChannelBanChecker: no ban record, cid={}, uid={}", cid, uid);
            return;
        }
        if (ban.isValid()) {
            if (soft) {
                markSoftFail(context, "banned");
                log.info("CPChannelBanChecker soft fail: user is banned, cid={}, uid={}, banId={}", cid, uid, ban.getId());
                return;
            }
            log.info("CPChannelBanChecker hard fail: user is banned, cid={}, uid={}, banId={}", cid, uid, ban.getId());
            fail(CPProblem.of(403, "user_muted", "user is banned in this channel"));
        }
        // 封禁已过期，尝试删除旧记录
        boolean deleted = channelBanDAO.delete(ban);
        if (deleted) {
            log.debug("CPChannelBanChecker: deleted expired ban, cid={}, uid={}, banId={}", cid, uid, ban.getId());
        } else {
            log.warn("CPChannelBanChecker: failed to delete expired ban, cid={}, uid={}, banId={}", cid, uid, ban.getId());
        }
        if (soft) {
            markSoftSuccess(context);
        }
    }
}
