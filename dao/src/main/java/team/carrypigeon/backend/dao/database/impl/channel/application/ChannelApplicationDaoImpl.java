package team.carrypigeon.backend.dao.database.impl.channel.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.api.bo.domain.channel.application.CPChannelApplication;
import team.carrypigeon.backend.api.dao.database.channel.application.ChannelApplicationDAO;
import team.carrypigeon.backend.dao.database.mapper.channel.application.ChannelApplicationMapper;
import team.carrypigeon.backend.dao.database.mapper.channel.application.ChannelApplicationPO;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class ChannelApplicationDaoImpl implements ChannelApplicationDAO {

    private final ChannelApplicationMapper channelApplicationMapper;

    public ChannelApplicationDaoImpl(ChannelApplicationMapper channelApplicationMapper) {
        this.channelApplicationMapper = channelApplicationMapper;
    }

    @Override
    @Cacheable(cacheNames = "channelApplicationById", key = "#id", unless = "#result == null")
    public CPChannelApplication getById(long id) {
        log.debug("ChannelApplicationDaoImpl#getById - id={}", id);
        CPChannelApplication result = Optional.ofNullable(channelApplicationMapper.selectById(id))
                .map(ChannelApplicationPO::toBo)
                .orElse(null);
        if (result == null) {
            log.debug("ChannelApplicationDaoImpl#getById - application not found, id={}", id);
        }
        return result;
    }

    @Override
    @Cacheable(cacheNames = "channelApplicationByUidCid", key = "#uid + ':' + #cid", unless = "#result == null")
    public CPChannelApplication getByUidAndCid(long uid, long cid) {
        log.debug("ChannelApplicationDaoImpl#getByUidAndCid - uid={}, cid={}", uid, cid);
        LambdaQueryWrapper<ChannelApplicationPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChannelApplicationPO::getUid, uid).eq(ChannelApplicationPO::getCid, cid);
        CPChannelApplication result = Optional.ofNullable(channelApplicationMapper.selectOne(queryWrapper))
                .map(ChannelApplicationPO::toBo)
                .orElse(null);
        if (result == null) {
            log.debug("ChannelApplicationDaoImpl#getByUidAndCid - application not found, uid={}, cid={}", uid, cid);
        }
        return result;
    }

    @Override
    public CPChannelApplication[] getByCid(long cid, int page, int pageSize) {
        log.debug("ChannelApplicationDaoImpl#getByCid - cid={}, page={}, pageSize={}", cid, page, pageSize);
        Page<ChannelApplicationPO> queryPage = new Page<>(page, pageSize);
        LambdaQueryWrapper<ChannelApplicationPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChannelApplicationPO::getCid, cid);
        List<ChannelApplicationPO> records = channelApplicationMapper.selectPage(queryPage, queryWrapper).getRecords();
        CPChannelApplication[] result = records.stream()
                .map(ChannelApplicationPO::toBo)
                .toArray(CPChannelApplication[]::new);
        log.debug("ChannelApplicationDaoImpl#getByCid - resultCount={}, cid={}", result.length, cid);
        return result;
    }

    @Override
    @CacheEvict(cacheNames = {"channelApplicationById", "channelApplicationByUidCid"}, allEntries = true)
    public boolean save(CPChannelApplication channelApplication) {
        if (channelApplication == null) {
            log.error("ChannelApplicationDaoImpl#save called with null channelApplication");
            return false;
        }
        boolean success = channelApplicationMapper.insertOrUpdate(ChannelApplicationPO.fromBo(channelApplication));
        if (success) {
            log.debug("ChannelApplicationDaoImpl#save success, id={}, cid={}, uid={}", channelApplication.getId(), channelApplication.getCid(), channelApplication.getUid());
        } else {
            log.warn("ChannelApplicationDaoImpl#save failed, id={}, cid={}, uid={}", channelApplication.getId(), channelApplication.getCid(), channelApplication.getUid());
        }
        return success;
    }
}
