package team.carrypigeon.backend.dao.database.impl.channel.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.api.bo.domain.channel.application.CPChannelApplication;
import team.carrypigeon.backend.api.dao.database.channel.application.ChannelApplicationDAO;
import team.carrypigeon.backend.dao.database.mapper.channel.application.ChannelApplicationMapper;
import team.carrypigeon.backend.dao.database.mapper.channel.application.ChannelApplicationPO;

import java.util.Optional;

@Service
public class ChannelApplicationDaoImpl implements ChannelApplicationDAO {

    private final ChannelApplicationMapper channelApplicationMapper;

    public ChannelApplicationDaoImpl(ChannelApplicationMapper channelApplicationMapper) {
        this.channelApplicationMapper = channelApplicationMapper;
    }

    @Override
    public CPChannelApplication getById(long id) {
        return Optional.ofNullable(channelApplicationMapper.selectById(id)).map(ChannelApplicationPO::toBo).orElse(null);
    }

    @Override
    public CPChannelApplication getByUidAndCid(long uid, long cid) {
        LambdaQueryWrapper<ChannelApplicationPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChannelApplicationPO::getUid, uid).eq(ChannelApplicationPO::getCid, cid);
        return Optional.ofNullable(channelApplicationMapper.selectOne(queryWrapper)).map(ChannelApplicationPO::toBo).orElse(null);
    }

    @Override
    public CPChannelApplication[] getByCid(long cid, int page, int pageSize) {
        Page<ChannelApplicationPO> queryPage = new Page<>(page, pageSize);
        LambdaQueryWrapper<ChannelApplicationPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChannelApplicationPO::getCid, cid);
        return channelApplicationMapper.selectPage(queryPage, queryWrapper).getRecords().stream().map(ChannelApplicationPO::toBo).toArray(CPChannelApplication[]::new);
    }

    @Override
    public boolean save(CPChannelApplication channelApplication) {
        return channelApplicationMapper.insertOrUpdate(ChannelApplicationPO.fromBo(channelApplication));
    }
}
