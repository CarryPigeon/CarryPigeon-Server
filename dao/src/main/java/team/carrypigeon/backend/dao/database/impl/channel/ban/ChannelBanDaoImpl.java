package team.carrypigeon.backend.dao.database.impl.channel.ban;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
    @Cacheable(cacheNames = "channelBanById", key = "#id")
    public CPChannelBan getById(long id) {
        return Optional.ofNullable(channelBanMapper.selectById(id)).map(ChannelBanPO::toBo).orElse( null);
    }

    @Override
    @Cacheable(cacheNames = "channelBanByCid", key = "#cid")
    public CPChannelBan[] getByChannelId(long cid) {
        LambdaQueryWrapper<ChannelBanPO> channelBanPOQueryWrapper = new LambdaQueryWrapper<>();
        channelBanPOQueryWrapper.eq(ChannelBanPO::getCid, cid);
        return channelBanMapper.selectList(channelBanPOQueryWrapper).stream().map(ChannelBanPO::toBo).toArray(CPChannelBan[]::new);
    }

    @Override
    @Cacheable(cacheNames = "channelBanByUidCid", key = "#uid + ':' + #cid")
    public CPChannelBan getByChannelIdAndUserId(long uid, long cid) {
        LambdaQueryWrapper<ChannelBanPO> channelBanPOQueryWrapper = new LambdaQueryWrapper<>();
        channelBanPOQueryWrapper.eq(ChannelBanPO::getCid, cid);
        channelBanPOQueryWrapper.eq(ChannelBanPO::getUid, uid);
        return Optional.ofNullable(channelBanMapper.selectOne(channelBanPOQueryWrapper)).map(ChannelBanPO::toBo).orElse(null);
    }

    @Override
    @CacheEvict(cacheNames = {"channelBanById", "channelBanByCid", "channelBanByUidCid"}, allEntries = true)
    public boolean save(CPChannelBan channelBan) {
        return channelBanMapper.insertOrUpdate(ChannelBanPO.fromBo(channelBan));
    }

    @Override
    @CacheEvict(cacheNames = {"channelBanById", "channelBanByCid", "channelBanByUidCid"}, allEntries = true)
    public boolean delete(CPChannelBan channelBan) {
        return channelBanMapper.deleteById(channelBan.getId())!=0;
    }
}
