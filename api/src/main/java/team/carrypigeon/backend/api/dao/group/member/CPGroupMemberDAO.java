package team.carrypigeon.backend.api.dao.group.member;

import team.carrypigeon.backend.api.bo.domain.group.CPGroupBO;
import team.carrypigeon.backend.api.bo.domain.group.member.CPGroupMemberBO;

/**
 * 群聊成员DAO接口
 * */
public interface CPGroupMemberDAO {
    /**
     * 通过群聊id获取所有群成员
     * */
    CPGroupMemberBO[] getMembers(long gid);
    /**
     * 通过用户id获取所有私有群组
     * */
    CPGroupBO[] getPrivateGroups(long uid);
    /**
     * 通过用户id和群聊id获取群聊
     * */
    CPGroupMemberBO getMember(long uid, long gid);
    /**
     * 更改用户权限状态
     * */
    boolean updateMember(long gid, long uid, int authority);
    /**
     * 向群组发送申请
     * */
    boolean createApply(long gid, long uid);
    /**
     * 更改用户状态
     * */
    boolean updateState(long gid, long uid, int state);
}