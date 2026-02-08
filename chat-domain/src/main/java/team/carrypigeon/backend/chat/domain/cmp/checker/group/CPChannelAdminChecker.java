package team.carrypigeon.backend.chat.domain.cmp.checker.group;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMemberAuthorityEnum;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.AbstractCheckerNode;
import team.carrypigeon.backend.api.dao.database.channel.ChannelDao;
import team.carrypigeon.backend.api.dao.database.channel.member.ChannelMemberDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeUserKeys;

/**
 * 校验当前用户是否为频道管理员的节点。<br/>
 * 输入：<br/>
 * 1. ChannelInfo_Id:Long<br/>
 * 2. UserInfo_Id:Long<br/>
 * 输出：<br/>
 * <ul>
 *     <li>hard 模式：校验失败时抛出 {@code 403 forbidden} 并中断流程</li>
 *     <li>soft 模式（bind type=soft）：仅写入 {@link CheckResult}，不抛异常</li>
 * </ul>
 * @author midreamsheep
 */
@Slf4j
@AllArgsConstructor
@LiteflowComponent("CPChannelAdminChecker")
public class CPChannelAdminChecker extends AbstractCheckerNode {

    private final ChannelDao channelDao;
    private final ChannelMemberDao channelMemberDao;

    @Override
    public void process(CPSession session, CPFlowContext context) throws Exception {
        boolean soft = isSoftMode();

        // 必填参数：ChannelInfo_Id, UserInfo_Id
        Long channelId = requireContext(context, CPNodeChannelKeys.CHANNEL_INFO_ID);
        Long userInfoId = requireContext(context, CPNodeUserKeys.USER_INFO_ID);

        CPChannel channelInfo = context.get(CPNodeChannelKeys.CHANNEL_INFO);
        if (channelInfo == null) {
            channelInfo = select(context,
                    buildSelectKey("channel", "id", channelId),
                    () -> channelDao.getById(channelId));
        }
        Long ownerUid = context.get(CPNodeChannelKeys.CHANNEL_INFO_OWNER);
        if ((channelInfo != null && channelInfo.getOwner() == userInfoId)
                || (ownerUid != null && ownerUid.equals(userInfoId))) {
            if (soft) {
                markSoftSuccess(context);
                log.debug("CPChannelAdminChecker soft success(owner), uid={}, cid={}", userInfoId, channelId);
            }
            return;
        }

        CPChannelMember channelMemberInfo = select(context,
                buildSelectKey("channel_member", java.util.Map.of("cid", channelId, "uid", userInfoId)),
                () -> channelMemberDao.getMember(userInfoId, channelId));
        if (channelMemberInfo == null) {
            if (soft) {
                markSoftFail(context, "not in channel");
                log.info("CPChannelAdminChecker soft fail: user not in channel, uid={}, cid={}", userInfoId, channelId);
                return;
            }
            log.info("CPChannelAdminChecker hard fail: user not in channel, uid={}, cid={}", userInfoId, channelId);
            forbidden("not_channel_member", "you are not in this channel");
            return;
        }
        if (channelMemberInfo.getAuthority() != CPChannelMemberAuthorityEnum.ADMIN) {
            if (soft) {
                markSoftFail(context, "not admin");
                log.info("CPChannelAdminChecker soft fail: user not admin, uid={}, cid={}", userInfoId, channelId);
                return;
            }
            log.info("CPChannelAdminChecker hard fail: user not admin, uid={}, cid={}", userInfoId, channelId);
            forbidden("not_channel_admin", "you are not the admin of this channel");
        }
        if (soft) {
            markSoftSuccess(context);
            log.debug("CPChannelAdminChecker soft success, uid={}, cid={}", userInfoId, channelId);
        }
    }
}
