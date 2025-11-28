package team.carrypigeon.backend.chat.domain.cmp.checker.group;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.chat.domain.controller.CPNodeComponent;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyBasicConstants;
import team.carrypigeon.backend.chat.domain.cmp.info.CheckResult;

/**
 * ?????????????Node<br/>
 * ??: ChannelInfo:{@link CPChannel};UserInfo_Id:Long<br/>
 * ???
 * <ul>
 *     <li>???????????? owner ???????</li>
 *     <li>??????bind type=soft???? CheckResult</li>
 * </ul>
 * @author midreamsheep
 */
@Slf4j
@LiteflowComponent("CPChannelOwnerChecker")
public class CPChannelOwnerChecker extends CPNodeComponent {

    private static final String BIND_TYPE_KEY = "type";

    @Override
    protected void process(CPSession session, DefaultContext context) throws Exception {
        String type = getBindData(BIND_TYPE_KEY, String.class);
        boolean soft = "soft".equalsIgnoreCase(type);

        CPChannel channelInfo = context.getData(CPNodeValueKeyBasicConstants.CHANNEL_INFO);
        Long userInfoId = context.getData(CPNodeValueKeyBasicConstants.USER_INFO_ID);
        if (channelInfo == null || userInfoId == null) {
            log.error("CPChannelOwnerChecker args error: ChannelInfo or UserInfo_Id is null");
            argsError(context);
            return;
        }
        if (channelInfo.getOwner() != userInfoId) {
            if (soft) {
                context.setData(CPNodeValueKeyBasicConstants.CHECK_RESULT,
                        new CheckResult(false, "not owner"));
                log.info("CPChannelOwnerChecker soft fail: user not owner, uid={}, cid={}", userInfoId, channelInfo.getId());
                return;
            }
            context.setData(CPNodeValueKeyBasicConstants.RESPONSE,
                    CPResponse.ERROR_RESPONSE.copy().setTextData("you are not the owner of this channel"));
            log.info("CPChannelOwnerChecker hard fail: user not owner, uid={}, cid={}", userInfoId, channelInfo.getId());
            throw new CPReturnException();
        }
        if (soft) {
            context.setData(CPNodeValueKeyBasicConstants.CHECK_RESULT, new CheckResult(true, null));
            log.debug("CPChannelOwnerChecker soft success, uid={}, cid={}", userInfoId, channelInfo.getId());
        }
    }
}
