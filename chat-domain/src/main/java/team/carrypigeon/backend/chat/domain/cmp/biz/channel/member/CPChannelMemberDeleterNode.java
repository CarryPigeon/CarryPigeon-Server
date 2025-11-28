package team.carrypigeon.backend.chat.domain.cmp.biz.channel.member;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMemberAuthorityEnum;
import team.carrypigeon.backend.api.chat.domain.controller.CPNodeComponent;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.dao.database.channel.member.ChannelMemberDao;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyBasicConstants;

/**
 * 删除频道成员的组件。<br/>
 * 不允许删除频道管理员。<br/>
 * 入参：ChannelMemberInfo:{@link CPChannelMember}<br/>
 * 出参：无
 */
@Slf4j
@AllArgsConstructor
@LiteflowComponent("CPChannelMemberDeleter")
public class CPChannelMemberDeleterNode extends CPNodeComponent {

    private final ChannelMemberDao channelMemberDao;

    @Override
    protected void process(CPSession session, DefaultContext context) throws Exception {
        CPChannelMember member = context.getData(CPNodeValueKeyBasicConstants.CHANNEL_MEMBER_INFO);
        if (member == null) {
            log.error("CPChannelMemberDeleter args error: ChannelMemberInfo is null");
            argsError(context);
            return;
        }
        if (member.getAuthority() == CPChannelMemberAuthorityEnum.ADMIN) {
            log.info("CPChannelMemberDeleter refuse to delete admin, uid={}, cid={}",
                    member.getUid(), member.getCid());
            context.setData(CPNodeValueKeyBasicConstants.RESPONSE,
                    CPResponse.ERROR_RESPONSE.copy().setTextData("cannot delete channel admin"));
            throw new CPReturnException();
        }
        if (!channelMemberDao.delete(member)) {
            log.error("CPChannelMemberDeleter delete failed, uid={}, cid={}",
                    member.getUid(), member.getCid());
            context.setData(CPNodeValueKeyBasicConstants.RESPONSE,
                    CPResponse.ERROR_RESPONSE.copy().setTextData("error deleting channel member"));
            throw new CPReturnException();
        }
        log.info("CPChannelMemberDeleter success, uid={}, cid={}", member.getUid(), member.getCid());
    }
}
