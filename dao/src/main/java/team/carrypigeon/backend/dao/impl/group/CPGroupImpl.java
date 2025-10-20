package team.carrypigeon.backend.dao.impl.group;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Component;
import team.carrypigeon.backend.api.bo.domain.group.CPGroupBO;
import team.carrypigeon.backend.api.dao.group.CPGroupDAO;
import team.carrypigeon.backend.dao.mapper.group.GroupMapper;
import team.carrypigeon.backend.dao.mapper.group.GroupPO;

@Component
public class CPGroupImpl implements CPGroupDAO {

    private final GroupMapper groupMapper;

    public CPGroupImpl(GroupMapper groupMapper) {
        this.groupMapper = groupMapper;
    }

    @Override
    public CPGroupBO getById(long id) {
        return groupMapper.selectById(id).toGroupBO();
    }

    @Override
    public CPGroupBO[] getFixedGroups() {
        QueryWrapper<GroupPO> queryWrapper = new QueryWrapper<GroupPO>()
                .eq("owner", -1);
        GroupPO[] groupPOS = groupMapper.selectList(queryWrapper).toArray(new GroupPO[0]);
        CPGroupBO[] groupBOs = new CPGroupBO[groupPOS.length];
        for (int i = 0; i < groupPOS.length; i++) {
            groupBOs[i] = groupPOS[i].toGroupBO();
        }
        return groupBOs;
    }

    @Override
    public boolean createGroup(CPGroupBO group) {
        GroupPO groupPO = new GroupPO();
        groupPO.fillData(group);
        groupMapper.insert(groupPO);
        return true;
    }

    @Override
    public boolean deleteGroup(long id) {
        return groupMapper.deleteById(id) > 0;
    }

    @Override
    public boolean updateGroup(CPGroupBO group) {
        GroupPO groupPO = new GroupPO();
        groupPO.fillData(group);
        return groupMapper.updateById(groupPO) > 0;
    }
}
