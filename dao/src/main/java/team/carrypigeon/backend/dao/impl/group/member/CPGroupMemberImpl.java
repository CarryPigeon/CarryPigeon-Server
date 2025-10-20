package team.carrypigeon.backend.dao.impl.group.member;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Component;
import team.carrypigeon.backend.api.bo.domain.group.CPGroupBO;
import team.carrypigeon.backend.api.bo.domain.group.member.CPGroupMemberBO;
import team.carrypigeon.backend.api.dao.group.member.CPGroupMemberDAO;
import team.carrypigeon.backend.common.id.IdUtil;
import team.carrypigeon.backend.dao.mapper.group.GroupMapper;
import team.carrypigeon.backend.dao.mapper.group.GroupPO;
import team.carrypigeon.backend.dao.mapper.group.member.GroupMemberMapper;
import team.carrypigeon.backend.dao.mapper.group.member.GroupMemberPO;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;

/**
 * 群聊成员表
 * */
@Component
public class CPGroupMemberImpl implements CPGroupMemberDAO {

    private final GroupMemberMapper groupMemberMapper;

    private final GroupMapper groupMapper;

    public CPGroupMemberImpl(GroupMemberMapper groupMemberMapper, GroupMapper groupMapper) {
        this.groupMemberMapper = groupMemberMapper;
        this.groupMapper = groupMapper;
    }

    @Override
    public CPGroupMemberBO[] getMembers(long gid) {
        QueryWrapper<GroupMemberPO> queryWrapper = new QueryWrapper<GroupMemberPO>()
                .eq("gid", gid);
        List<GroupMemberPO> groupMemberPOS =
                groupMemberMapper.selectList(queryWrapper);
        CPGroupMemberBO[] groupMemberBOS = new CPGroupMemberBO[groupMemberPOS.size()];
        for (int i = 0; i < groupMemberPOS.size(); i++) {
            groupMemberBOS[i] = groupMemberPOS.get(i).toBO();
        }
        return groupMemberBOS;
    }

    @Override
    public CPGroupBO[] getPrivateGroups(long uid) {
        QueryWrapper<GroupMemberPO> queryWrapper = new QueryWrapper<GroupMemberPO>()
                .eq("uid", uid)
                .eq("state", 2);
        List<GroupMemberPO> groupMemberPOS =
                groupMemberMapper.selectList(queryWrapper);
        CPGroupBO[] groupBOS = new CPGroupBO[groupMemberPOS.size()];
        for (int i = 0; i < groupMemberPOS.size(); i++) {
            groupBOS[i] = groupMapper.selectById(groupMemberPOS.get(i).getGid()).toGroupBO();
        }
        return groupBOS;
    }

    @Override
    public CPGroupMemberBO getMember(long uid, long gid) {
        QueryWrapper<GroupMemberPO> queryWrapper = new QueryWrapper<GroupMemberPO>()
                .eq("uid", uid)
                .eq("gid", gid);
        return groupMemberMapper.selectOne(queryWrapper).toBO();
    }

    @Override
    public Long[] getAdmins(long gid) {
        GroupPO groupPO = groupMapper.selectById(gid);
        if (groupPO == null){
            return new Long[0];
        }
        LinkedList<Long> admins = new LinkedList<>();
        admins.add(groupPO.getOwner());
        for (CPGroupMemberBO member : getMembers(gid)) {
            if (member.getAuthority() == 2) {
                admins.add(member.getUid());
            }
        }
        return admins.toArray(new Long[0]);
    }

    @Override
    public boolean updateAuthority(long gid, long uid, int authority) {
        QueryWrapper<GroupMemberPO> queryWrapper = new QueryWrapper<GroupMemberPO>()
                .eq("gid", gid)
                .eq("uid", uid);
        GroupMemberPO groupMemberPO = groupMemberMapper.selectOne(queryWrapper);
        if (groupMemberPO != null) {
            groupMemberPO.setAuthority(authority);
            groupMemberMapper.updateById(groupMemberPO);
            return true;
        }
        return false;
    }

    @Override
    public boolean createApply(long gid, long uid) {
        GroupMemberPO groupMemberPO = new GroupMemberPO();
        groupMemberPO.setId(IdUtil.generateId());
        groupMemberPO.setGid(gid);
        groupMemberPO.setUid(uid);
        groupMemberPO.setState(1);
        groupMemberPO.setAuthority(1);
        groupMemberPO.setTime(LocalDateTime.now());
        return groupMemberMapper.insert(groupMemberPO) > 0;
    }

    @Override
    public boolean updateState(long gid, long uid, int state) {
        QueryWrapper<GroupMemberPO> queryWrapper = new QueryWrapper<GroupMemberPO>()
                .eq("gid", gid)
                .eq("uid", uid);
        GroupMemberPO groupMemberPO = groupMemberMapper.selectOne(queryWrapper);
        if (groupMemberPO != null) {
            groupMemberPO.setState(state);
            groupMemberMapper.updateById(groupMemberPO);
            return true;
        }
        return false;
    }

    @Override
    public boolean deleteMember(CPGroupMemberBO memberBO) {
        return groupMemberMapper.deleteById(memberBO.getId())>0;
    }
}
