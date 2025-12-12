package team.carrypigeon.backend.chat.domain.cmp.checker.group;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeBindKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeCommonKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeUserKeys;
import team.carrypigeon.backend.chat.domain.cmp.info.CheckResult;

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
public class CPChannelOwnerChecker extends CPNodeComponent {

    private static final String BIND_TYPE_KEY = CPNodeBindKeys.TYPE;

    @Override
    public void process(CPSession session, DefaultContext context) throws Exception {
        String type = getBindData(BIND_TYPE_KEY, String.class);
        boolean soft = "soft".equalsIgnoreCase(type);

        CPChannel channelInfo = context.getData(CPNodeChannelKeys.CHANNEL_INFO);
        Long userInfoId = context.getData(CPNodeUserKeys.USER_INFO_ID);
        if (channelInfo == null || userInfoId == null) {
            log.error("CPChannelOwnerChecker args error: ChannelInfo or UserInfo_Id is null");
            argsError(context);
            return;
        }
        if (channelInfo.getOwner() != userInfoId) {
            if (soft) {
                context.setData(CPNodeCommonKeys.CHECK_RESULT,
                        new CheckResult(false, "not owner"));
                log.info("CPChannelOwnerChecker soft fail: user not owner, uid={}, cid={}", userInfoId, channelInfo.getId());
                return;
            }
            context.setData(CPNodeCommonKeys.RESPONSE,
                    CPResponse.ERROR_RESPONSE.copy().setTextData("you are not the owner of this channel"));
            log.info("CPChannelOwnerChecker hard fail: user not owner, uid={}, cid={}", userInfoId, channelInfo.getId());
            throw new CPReturnException();
        }
        if (soft) {
            context.setData(CPNodeCommonKeys.CHECK_RESULT, new CheckResult(true, null));
            log.debug("CPChannelOwnerChecker soft success, uid={}, cid={}", userInfoId, channelInfo.getId());
        }
    }
}
