package team.carrypigeon.backend.dao.database.impl.channel;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.dao.database.channel.ChannelDao;
import team.carrypigeon.backend.dao.database.mapper.channel.ChannelMapper;
import team.carrypigeon.backend.dao.database.mapper.channel.ChannelPO;

import java.util.List;
import java.util.Optional;

/**
 * {@link ChannelDao} 的数据库实现（MyBatis-Plus + Spring Cache）。
 * <p>
 * {@link #getAllFixed()}：查询 owner=-1 的“固定频道”（由业务约定定义）。
 */
@Slf4j
@Service
public class ChannelDaoImpl implements ChannelDao {

    private final ChannelMapper channelMapper;

    /**
     * 创建 Channel DAO 实现（由 Spring 注入 {@link ChannelMapper}）。
     */
    public ChannelDaoImpl(ChannelMapper channelMapper) {
        this.channelMapper = channelMapper;
    }

    @Override
    @Cacheable(cacheNames = "channelById", key = "#id", unless = "#result == null")
    public CPChannel getById(long id) {
        log.debug("ChannelDaoImpl#getById - cid={}", id);
        CPChannel result = Optional.ofNullable(channelMapper.selectById(id))
                .map(ChannelPO::toBo)
                .orElse(null);
        if (result == null) {
            log.debug("ChannelDaoImpl#getById - channel not found, cid={}", id);
        }
        return result;
    }

    @Override
    @Cacheable(cacheNames = "channelFixed", key = "'all'")
    public CPChannel[] getAllFixed() {
        log.debug("ChannelDaoImpl#getAllFixed - query fixed channels");
        LambdaQueryWrapper<ChannelPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChannelPO::getOwner, -1);
        List<ChannelPO> list = channelMapper.selectList(queryWrapper);
        CPChannel[] result = list.stream()
                .map(ChannelPO::toBo)
                .toArray(CPChannel[]::new);
        log.debug("ChannelDaoImpl#getAllFixed - resultCount={}", result.length);
        return result;
    }

    @Override
    @CacheEvict(cacheNames = {"channelById", "channelFixed"}, allEntries = true)
    public boolean save(CPChannel channel) {
        if (channel == null) {
            log.error("ChannelDaoImpl#save called with null channel");
            return false;
        }
        boolean success = channelMapper.insertOrUpdate(ChannelPO.fromBo(channel));
        if (success) {
            log.debug("ChannelDaoImpl#save success, cid={}, owner={}", channel.getId(), channel.getOwner());
        } else {
            log.warn("ChannelDaoImpl#save failed, cid={}, owner={}", channel.getId(), channel.getOwner());
        }
        return success;
    }

    @Override
    @CacheEvict(cacheNames = {"channelById", "channelFixed"}, allEntries = true)
    public boolean delete(CPChannel cpChannel) {
        if (cpChannel == null) {
            log.error("ChannelDaoImpl#delete called with null channel");
            return false;
        }
        boolean success = channelMapper.deleteById(cpChannel.getId())!=0;
        if (success) {
            log.debug("ChannelDaoImpl#delete success, cid={}", cpChannel.getId());
        } else {
            log.warn("ChannelDaoImpl#delete failed, cid={}", cpChannel.getId());
        }
        return success;
    }
}
