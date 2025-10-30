package team.carrypigeon.backend.dao.database.impl.channel;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.dao.database.channel.ChannelDao;
import team.carrypigeon.backend.dao.database.mapper.channel.ChannelMapper;
import team.carrypigeon.backend.dao.database.mapper.channel.ChannelPO;

import java.util.List;
import java.util.Optional;

@Service
public class ChannelDaoImpl implements ChannelDao {

    private final ChannelMapper channelMapper;

    public ChannelDaoImpl(ChannelMapper channelMapper) {
        this.channelMapper = channelMapper;
    }

    @Override
    public CPChannel getById(long id) {
        return Optional.ofNullable(channelMapper.selectById(id)).map(ChannelPO::toBo).orElse(null);
    }

    @Override
    public CPChannel[] getAllFixed() {
        LambdaQueryWrapper<ChannelPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChannelPO::getOwner, -1);
        List<ChannelPO> list = channelMapper.selectList(queryWrapper);
        return list.stream()
                .map(ChannelPO::toBo)
                .toArray(CPChannel[]::new);
    }

    @Override
    public boolean save(CPChannel channel) {
        return channelMapper.insertOrUpdate(ChannelPO.fromBo(channel));
    }

    @Override
    public boolean delete(CPChannel cpChannel) {
        return channelMapper.deleteById(cpChannel.getId())!=0;
    }
}
