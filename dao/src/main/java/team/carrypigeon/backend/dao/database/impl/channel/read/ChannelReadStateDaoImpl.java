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
 * {@link ChannelReadStateDao} 的数据库实现。
 * <p>
 * 基于 MyBatis-Plus 完成读写，并通过 Spring Cache 缓存常见查询。
 */
@Slf4j
@Service
public class ChannelReadStateDaoImpl implements ChannelReadStateDao {

    private final ChannelReadStateMapper channelReadStateMapper;

    /**
     * 构造函数注入 Mapper。
     */
    public ChannelReadStateDaoImpl(ChannelReadStateMapper channelReadStateMapper) {
        this.channelReadStateMapper = channelReadStateMapper;
    }

    /**
     * 按主键查询数据。
     *
     * @param id 已读状态记录 ID
     * @return 匹配的已读状态；不存在时返回 {@code null}
     */
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

    /**
     * 按用户与频道联合查询数据。
     *
     * @param uid 用户 ID
     * @param cid 频道 ID
     * @return 匹配的已读状态；不存在时返回 {@code null}
     */
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

    /**
     * 保存频道已读状态。
     *
     * @param state 待保存的已读状态实体
     * @return {@code true} 表示写库成功
     */
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

    /**
     * 删除频道已读状态。
     *
     * @param state 待删除的已读状态实体
     * @return {@code true} 表示删除成功
     */
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
