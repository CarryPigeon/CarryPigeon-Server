package team.carrypigeon.backend.chat.domain.cmp.checker.group;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMemberAuthorityEnum;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.chat.domain.controller.CPNodeComponent;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.api.dao.database.channel.member.ChannelMemberDao;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyBasicConstants;
import team.carrypigeon.backend.chat.domain.cmp.info.CheckResult;

/**
 * 校验当前用户是否为频道管理员的节点。<br/>
 * 输入：<br/>
 * 1. ChannelInfo_Id:Long<br/>
 * 2. UserInfo_Id:Long<br/>
 * 输出：<br/>
 * <ul>
 *     <li>hard 模式：校验失败时写入错误响应并中断流程</li>
 *     <li>soft 模式（bind type=soft）：仅写入 {@link CheckResult}，不抛异常</li>
 * </ul>
 * @author midreamsheep
 */
@Slf4j
@AllArgsConstructor
@LiteflowComponent("CPChannelAdminChecker")
public class CPChannelAdminChecker extends CPNodeComponent {

    private static final String BIND_TYPE_KEY = "type";

    private final ChannelMemberDao channelMemberDao;

    @Override
    public void process(CPSession session, DefaultContext context) throws Exception {
        String type = getBindData(BIND_TYPE_KEY, String.class);
        boolean soft = "soft".equalsIgnoreCase(type);

        Long channelId = context.getData(CPNodeValueKeyBasicConstants.CHANNEL_INFO_ID);
        Long userInfoId = context.getData(CPNodeValueKeyBasicConstants.USER_INFO_ID);
        if (channelId == null || userInfoId == null) {
            log.error("CPChannelAdminChecker args error: ChannelInfo_Id or UserInfo_Id is null");
            argsError(context);
            return;
        }
        CPChannelMember channelMemberInfo = channelMemberDao.getMember(userInfoId, channelId);
        if (channelMemberInfo == null) {
            if (soft) {
                context.setData(CPNodeValueKeyBasicConstants.CHECK_RESULT,
                        new CheckResult(false, "not in channel"));
                log.info("CPChannelAdminChecker soft fail: user not in channel, uid={}, cid={}", userInfoId, channelId);
                return;
            }
            context.setData(CPNodeValueKeyBasicConstants.RESPONSE,
                    CPResponse.ERROR_RESPONSE.copy().setTextData("you are not in this channel"));
            log.info("CPChannelAdminChecker hard fail: user not in channel, uid={}, cid={}", userInfoId, channelId);
            throw new CPReturnException();
        }
        if (channelMemberInfo.getAuthority() != CPChannelMemberAuthorityEnum.ADMIN) {
            if (soft) {
                context.setData(CPNodeValueKeyBasicConstants.CHECK_RESULT,
                        new CheckResult(false, "not admin"));
                log.info("CPChannelAdminChecker soft fail: user not admin, uid={}, cid={}", userInfoId, channelId);
                return;
            }
            context.setData(CPNodeValueKeyBasicConstants.RESPONSE,
                    CPResponse.ERROR_RESPONSE.copy().setTextData("you are not the admin of this channel"));
            log.info("CPChannelAdminChecker hard fail: user not admin, uid={}, cid={}", userInfoId, channelId);
            throw new CPReturnException();
        }
        if (soft) {
            context.setData(CPNodeValueKeyBasicConstants.CHECK_RESULT, new CheckResult(true, null));
            log.debug("CPChannelAdminChecker soft success, uid={}, cid={}", userInfoId, channelId);
        }
    }
}
