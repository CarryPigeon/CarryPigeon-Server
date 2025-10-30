package team.carrypigeon.backend.dao.database.impl.channel.member;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.dao.database.channel.member.ChannelMemberDao;
import team.carrypigeon.backend.dao.database.mapper.channel.member.ChannelMemberMapper;
import team.carrypigeon.backend.dao.database.mapper.channel.member.ChannelMemberPO;

import java.util.List;
import java.util.Optional;

@Service
public class ChannelMemberDaoImpl implements ChannelMemberDao {

    private final ChannelMemberMapper channelMemberMapper;

    public ChannelMemberDaoImpl(ChannelMemberMapper channelMemberMapper) {
        this.channelMemberMapper = channelMemberMapper;
    }

    @Override
    public CPChannelMember getById(long id) {
        return Optional.ofNullable(channelMemberMapper.selectById(id)).map(ChannelMemberPO::toBo).orElse(null);
    }

    @Override
    public CPChannelMember[] getAllMember(long cid) {
        LambdaQueryWrapper<ChannelMemberPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChannelMemberPO::getCid, cid);
        List<ChannelMemberPO> channelMemberPOList = channelMemberMapper.selectList(queryWrapper);
        return channelMemberPOList.stream()
                .map(ChannelMemberPO::toBo)
                .toArray(CPChannelMember[]::new);
    }

    @Override
    public CPChannelMember getMember(long uid, long cid) {
        LambdaQueryWrapper<ChannelMemberPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChannelMemberPO::getUid, uid).eq(ChannelMemberPO::getCid, cid);
        return Optional.ofNullable(channelMemberMapper.selectOne(queryWrapper))
                .map(ChannelMemberPO::toBo)
                .orElse(null);
    }

    @Override
    public CPChannelMember[] getAllMemberByUserId(long uid) {
        LambdaQueryWrapper<ChannelMemberPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChannelMemberPO::getUid, uid);
        List<ChannelMemberPO> channelMemberPOList = channelMemberMapper.selectList(queryWrapper);
        return channelMemberPOList.stream()
                .map(ChannelMemberPO::toBo)
                .toArray(CPChannelMember[]::new);
    }

    @Override
    public boolean save(CPChannelMember channelMember) {
        return channelMemberMapper.insertOrUpdate(ChannelMemberPO.from(channelMember));
    }

    @Override
    public boolean delete(CPChannelMember cpChannelMember) {
        return channelMemberMapper.deleteById(cpChannelMember.getId())!=0;
    }
}
