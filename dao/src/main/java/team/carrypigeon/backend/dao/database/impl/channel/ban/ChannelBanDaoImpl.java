package team.carrypigeon.backend.dao.database.impl.channel.ban;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.api.bo.domain.channel.ban.CPChannelBan;
import team.carrypigeon.backend.api.dao.database.channel.ban.ChannelBanDAO;
import team.carrypigeon.backend.dao.database.mapper.channel.ban.ChannelBanMapper;
import team.carrypigeon.backend.dao.database.mapper.channel.ban.ChannelBanPO;

import java.util.Optional;

@Service
public class ChannelBanDaoImpl implements ChannelBanDAO {

    private final ChannelBanMapper channelBanMapper;

    public ChannelBanDaoImpl(ChannelBanMapper channelBanMapper) {
        this.channelBanMapper = channelBanMapper;
    }

    @Override
    public CPChannelBan getById(long id) {
        return Optional.ofNullable(channelBanMapper.selectById(id)).map(ChannelBanPO::toBo).orElse( null);
    }

    @Override
    public CPChannelBan[] getByChannelId(long cid) {
        LambdaQueryWrapper<ChannelBanPO> channelBanPOQueryWrapper = new LambdaQueryWrapper<>();
        channelBanPOQueryWrapper.eq(ChannelBanPO::getCid, cid);
        return channelBanMapper.selectList(channelBanPOQueryWrapper).stream().map(ChannelBanPO::toBo).toArray(CPChannelBan[]::new);
    }

    @Override
    public CPChannelBan getByChannelIdAndUserId(long uid, long cid) {
        LambdaQueryWrapper<ChannelBanPO> channelBanPOQueryWrapper = new LambdaQueryWrapper<>();
        channelBanPOQueryWrapper.eq(ChannelBanPO::getCid, cid);
        channelBanPOQueryWrapper.eq(ChannelBanPO::getUid, uid);
        return Optional.ofNullable(channelBanMapper.selectOne(channelBanPOQueryWrapper)).map(ChannelBanPO::toBo).orElse(null);
    }

    @Override
    public boolean save(CPChannelBan channelBan) {
        return channelBanMapper.insertOrUpdate(ChannelBanPO.fromBo(channelBan));
    }

    @Override
    public boolean delete(CPChannelBan channelBan) {
        return channelBanMapper.deleteById(channelBan.getId())!=0;
    }
}
