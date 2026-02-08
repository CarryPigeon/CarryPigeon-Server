package team.carrypigeon.backend.dao.database.impl.channel.read;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.api.bo.domain.channel.read.CPChannelReadState;
import team.carrypigeon.backend.api.dao.database.channel.read.ChannelReadStateDao;
import team.carrypigeon.backend.dao.database.mapper.channel.read.ChannelReadStateMapper;
import team.carrypigeon.backend.dao.database.mapper.channel.read.ChannelReadStatePO;

import java.util.Optional;

/**
 * Database implementation of {@link ChannelReadStateDao}.
 */
@Slf4j
@Service
public class ChannelReadStateDaoImpl implements ChannelReadStateDao {

    private final ChannelReadStateMapper channelReadStateMapper;

    /**
     * Create read-state DAO implementation.
     */
    public ChannelReadStateDaoImpl(ChannelReadStateMapper channelReadStateMapper) {
        this.channelReadStateMapper = channelReadStateMapper;
    }

    @Override
    @Cacheable(cacheNames = "channelReadStateById", key = "#id", unless = "#result == null")
    public CPChannelReadState getById(long id) {
        log.debug("ChannelReadStateDaoImpl#getById - id={}", id);
        CPChannelReadState result = Optional.ofNullable(channelReadStateMapper.selectById(id))
                .map(ChannelReadStatePO::toBo)
                .orElse(null);
        if (result == null) {
            log.debug("ChannelReadStateDaoImpl#getById - state not found, id={}", id);
        }
        return result;
    }

    @Override
    @Cacheable(cacheNames = "channelReadStateByUidCid", key = "#uid + ':' + #cid", unless = "#result == null")
    public CPChannelReadState getByUidAndCid(long uid, long cid) {
        log.debug("ChannelReadStateDaoImpl#getByUidAndCid - uid={}, cid={}", uid, cid);
        LambdaQueryWrapper<ChannelReadStatePO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChannelReadStatePO::getUid, uid)
                .eq(ChannelReadStatePO::getCid, cid);
        CPChannelReadState result = Optional.ofNullable(channelReadStateMapper.selectOne(queryWrapper))
                .map(ChannelReadStatePO::toBo)
                .orElse(null);
        if (result == null) {
            log.debug("ChannelReadStateDaoImpl#getByUidAndCid - state not found, uid={}, cid={}", uid, cid);
        }
        return result;
    }

    @Override
    @CacheEvict(cacheNames = {"channelReadStateById", "channelReadStateByUidCid"}, allEntries = true)
    public boolean save(CPChannelReadState state) {
        if (state == null) {
            log.error("ChannelReadStateDaoImpl#save called with null state");
            return false;
        }
        boolean success = channelReadStateMapper.insertOrUpdate(ChannelReadStatePO.fromBo(state));
        if (success) {
            log.debug("ChannelReadStateDaoImpl#save success, id={}, uid={}, cid={}",
                    state.getId(), state.getUid(), state.getCid());
        } else {
            log.warn("ChannelReadStateDaoImpl#save failed, id={}, uid={}, cid={}",
                    state.getId(), state.getUid(), state.getCid());
        }
        return success;
    }

    @Override
    @CacheEvict(cacheNames = {"channelReadStateById", "channelReadStateByUidCid"}, allEntries = true)
    public boolean delete(CPChannelReadState state) {
        if (state == null) {
            log.error("ChannelReadStateDaoImpl#delete called with null state");
            return false;
        }
        boolean success = channelReadStateMapper.deleteById(state.getId()) != 0;
        if (success) {
            log.debug("ChannelReadStateDaoImpl#delete success, id={}", state.getId());
        } else {
            log.warn("ChannelReadStateDaoImpl#delete failed, id={}", state.getId());
        }
        return success;
    }
}
