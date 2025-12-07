package team.carrypigeon.backend.dao.database.impl.channel.ban;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.api.bo.domain.channel.ban.CPChannelBan;
import team.carrypigeon.backend.api.dao.database.channel.ban.ChannelBanDAO;
import team.carrypigeon.backend.dao.database.mapper.channel.ban.ChannelBanMapper;
import team.carrypigeon.backend.dao.database.mapper.channel.ban.ChannelBanPO;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class ChannelBanDaoImpl implements ChannelBanDAO {

    private final ChannelBanMapper channelBanMapper;

    public ChannelBanDaoImpl(ChannelBanMapper channelBanMapper) {
        this.channelBanMapper = channelBanMapper;
    }

    @Override
    @Cacheable(cacheNames = "channelBanById", key = "#id")
    public CPChannelBan getById(long id) {
        log.debug("ChannelBanDaoImpl#getById - id={}", id);
        CPChannelBan result = Optional.ofNullable(channelBanMapper.selectById(id))
                .map(ChannelBanPO::toBo)
                .orElse( null);
        if (result == null) {
            log.debug("ChannelBanDaoImpl#getById - ban not found, id={}", id);
        }
        return result;
    }

    @Override
    @Cacheable(cacheNames = "channelBanByCid", key = "#cid")
    public CPChannelBan[] getByChannelId(long cid) {
        log.debug("ChannelBanDaoImpl#getByChannelId - cid={}", cid);
        LambdaQueryWrapper<ChannelBanPO> channelBanPOQueryWrapper = new LambdaQueryWrapper<>();
        channelBanPOQueryWrapper.eq(ChannelBanPO::getCid, cid);
        List<ChannelBanPO> list = channelBanMapper.selectList(channelBanPOQueryWrapper);
        CPChannelBan[] result = list.stream()
                .map(ChannelBanPO::toBo)
                .toArray(CPChannelBan[]::new);
        log.debug("ChannelBanDaoImpl#getByChannelId - resultCount={}, cid={}", result.length, cid);
        return result;
    }

    @Override
    @Cacheable(cacheNames = "channelBanByUidCid", key = "#uid + ':' + #cid")
    public CPChannelBan getByChannelIdAndUserId(long uid, long cid) {
        log.debug("ChannelBanDaoImpl#getByChannelIdAndUserId - uid={}, cid={}", uid, cid);
        LambdaQueryWrapper<ChannelBanPO> channelBanPOQueryWrapper = new LambdaQueryWrapper<>();
        channelBanPOQueryWrapper.eq(ChannelBanPO::getCid, cid);
        channelBanPOQueryWrapper.eq(ChannelBanPO::getUid, uid);
        CPChannelBan result = Optional.ofNullable(channelBanMapper.selectOne(channelBanPOQueryWrapper))
                .map(ChannelBanPO::toBo)
                .orElse(null);
        if (result == null) {
            log.debug("ChannelBanDaoImpl#getByChannelIdAndUserId - ban not found, uid={}, cid={}", uid, cid);
        }
        return result;
    }

    @Override
    @CacheEvict(cacheNames = {"channelBanById", "channelBanByCid", "channelBanByUidCid"}, allEntries = true)
    public boolean save(CPChannelBan channelBan) {
        if (channelBan == null) {
            log.error("ChannelBanDaoImpl#save called with null channelBan");
            return false;
        }
        boolean success = channelBanMapper.insertOrUpdate(ChannelBanPO.fromBo(channelBan));
        if (success) {
            log.debug("ChannelBanDaoImpl#save success, id={}, cid={}, uid={}", channelBan.getId(), channelBan.getCid(), channelBan.getUid());
        } else {
            log.warn("ChannelBanDaoImpl#save failed, id={}, cid={}, uid={}", channelBan.getId(), channelBan.getCid(), channelBan.getUid());
        }
        return success;
    }

    @Override
    @CacheEvict(cacheNames = {"channelBanById", "channelBanByCid", "channelBanByUidCid"}, allEntries = true)
    public boolean delete(CPChannelBan channelBan) {
        if (channelBan == null) {
            log.error("ChannelBanDaoImpl#delete called with null channelBan");
            return false;
        }
        boolean success = channelBanMapper.deleteById(channelBan.getId())!=0;
        if (success) {
            log.debug("ChannelBanDaoImpl#delete success, id={}", channelBan.getId());
        } else {
            log.warn("ChannelBanDaoImpl#delete failed, id={}", channelBan.getId());
        }
        return success;
    }
}
