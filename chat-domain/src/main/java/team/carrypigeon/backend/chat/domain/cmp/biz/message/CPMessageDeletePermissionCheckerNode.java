package team.carrypigeon.backend.chat.domain.cmp.biz.message;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMemberAuthorityEnum;
import team.carrypigeon.backend.api.bo.domain.message.CPMessage;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.dao.database.channel.member.ChannelMemberDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeCommonKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeMessageKeys;
import team.carrypigeon.backend.common.time.TimeUtil;

import java.time.LocalDateTime;

/**
 * 删除消息权限校验组件。<br/>
 * 规则：
 *  - 在发送后限定时间窗口内（默认 120 秒）；
 *  - 发送者本人可以删除；
 *  - 频道管理员可以删除任何消息。
 */
@Slf4j
@AllArgsConstructor
@LiteflowComponent("CPMessageDeletePermissionChecker")
public class CPMessageDeletePermissionCheckerNode extends CPNodeComponent {

    private static final long DELETE_WINDOW_SECONDS = 120L;

    private final ChannelMemberDao channelMemberDao;

    @Override
    public void process(CPSession session, DefaultContext context) throws Exception {
        CPMessage message = context.getData(CPNodeMessageKeys.MESSAGE_INFO);
        Long operatorUid = context.getData(CPNodeCommonKeys.SESSION_ID);
        if (message == null || operatorUid == null) {
            log.error("CPMessageDeletePermissionChecker args error: message or operatorUid is null");
            argsError(context);
            return;
        }
        LocalDateTime sendTime = message.getSendTime();
        if (sendTime == null) {
            log.error("CPMessageDeletePermissionChecker error: message sendTime is null, mid={}", message.getId());
            argsError(context);
            return;
        }
        LocalDateTime now = TimeUtil.getCurrentLocalTime();
        if (now.isAfter(sendTime.plusSeconds(DELETE_WINDOW_SECONDS))) {
            log.info("CPMessageDeletePermissionChecker fail: delete timeout, mid={}, operatorUid={}",
                    message.getId(), operatorUid);
            context.setData(CPNodeCommonKeys.RESPONSE,
                    CPResponse.ERROR_RESPONSE.copy().setTextData("message delete timeout"));
            throw new CPReturnException();
        }
        // 发送者本人可以删除
        if (operatorUid.equals(message.getUid())) {
            log.debug("CPMessageDeletePermissionChecker success: owner delete, mid={}, uid={}",
                    message.getId(), operatorUid);
            return;
        }
        // 非发送者需要检查是否为频道管理员
        CPChannelMember member = channelMemberDao.getMember(operatorUid, message.getCid());
        if (member == null || member.getAuthority() != CPChannelMemberAuthorityEnum.ADMIN) {
            log.info("CPMessageDeletePermissionChecker fail: no permission, mid={}, operatorUid={}",
                    message.getId(), operatorUid);
            context.setData(CPNodeCommonKeys.RESPONSE,
                    CPResponse.ERROR_RESPONSE.copy().setTextData("no permission to delete message"));
            throw new CPReturnException();
        }
        log.debug("CPMessageDeletePermissionChecker success: admin delete, mid={}, operatorUid={}",
                message.getId(), operatorUid);
    }
}
