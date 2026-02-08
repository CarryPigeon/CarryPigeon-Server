package team.carrypigeon.backend.chat.domain.cmp.checker.group;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.AbstractCheckerNode;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeUserKeys;

/**
 * 校验当前用户是否为频道 owner 的节点。<br/>
 * 输入：ChannelInfo:{@link CPChannel}; UserInfo_Id:Long<br/>
 * 输出：<br/>
 * <ul>
 *     <li>hard 模式：当前用户不是 owner 时直接写入错误响应并中断</li>
 *     <li>soft 模式（bind type=soft）：仅将结果写入 {@link CheckResult}</li>
 * </ul>
 * @author midreamsheep
 */
@Slf4j
@LiteflowComponent("CPChannelOwnerChecker")
public class CPChannelOwnerChecker extends AbstractCheckerNode {

    @Override
    public void process(CPSession session, CPFlowContext context) throws Exception {
        boolean soft = isSoftMode();

        // 必填参数：ChannelInfo, UserInfo_Id
        CPChannel channelInfo = requireContext(context, CPNodeChannelKeys.CHANNEL_INFO);
        Long userInfoId = requireContext(context, CPNodeUserKeys.USER_INFO_ID);
        if (channelInfo.getOwner() != userInfoId) {
            if (soft) {
                markSoftFail(context, "not owner");
                log.info("CPChannelOwnerChecker soft fail: user not owner, uid={}, cid={}", userInfoId, channelInfo.getId());
                return;
            }
            log.info("CPChannelOwnerChecker hard fail: user not owner, uid={}, cid={}", userInfoId, channelInfo.getId());
            forbidden("not_channel_owner", "you are not the owner of this channel");
        }
        if (soft) {
            markSoftSuccess(context);
            log.debug("CPChannelOwnerChecker soft success, uid={}, cid={}", userInfoId, channelInfo.getId());
        }
    }
}
