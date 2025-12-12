package team.carrypigeon.backend.chat.domain.cmp.biz.channel.member;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.chat.domain.node.AbstractSaveNode;
import team.carrypigeon.backend.api.dao.database.channel.member.ChannelMemberDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelMemberKeys;

/**
 * 用于保存频道成员的Node<br/>
 * 入参: ChannelMemberInfo:{@link CPChannelMember}<br/>
 * 出参: 无<br/>
 * @author midreamsheep
 * */
@AllArgsConstructor
@LiteflowComponent("CPChannelMemberSaver")
public class CPChannelMemberSaverNode extends AbstractSaveNode<CPChannelMember> {

    private final ChannelMemberDao channelMemberDao;

    @Override
    protected String getContextKey() {
        return CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO;
    }

    @Override
    protected Class<CPChannelMember> getEntityClass() {
        return CPChannelMember.class;
    }

    @Override
    protected boolean doSave(CPChannelMember entity) {
        return channelMemberDao.save(entity);
    }

    @Override
    protected String getErrorMessage() {
        return "save channel member error";
    }
}
