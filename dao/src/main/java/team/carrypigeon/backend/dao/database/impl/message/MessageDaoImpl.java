package team.carrypigeon.backend.dao.database.impl.message;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.api.bo.domain.message.CPMessage;
import team.carrypigeon.backend.api.dao.database.message.ChannelMessageDao;
import team.carrypigeon.backend.dao.database.mapper.message.MessageMapper;
import team.carrypigeon.backend.dao.database.mapper.message.MessagePO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class MessageDaoImpl implements ChannelMessageDao {

    private final MessageMapper messageMapper;

    public MessageDaoImpl(MessageMapper messageMapper) {
        this.messageMapper = messageMapper;
    }

    @Override
    @Cacheable(cacheNames = "messageById", key = "#id", unless = "#result == null")
    public CPMessage getById(long id) {
        log.debug("MessageDaoImpl#getById - id={}", id);
        CPMessage result = Optional.ofNullable(messageMapper.selectById(id))
                .map(MessagePO::toBo)
                .orElse(null);
        if (result == null) {
            log.debug("MessageDaoImpl#getById - message not found, id={}", id);
        }
        return result;
    }

    @Override
    public CPMessage[] getBefore(long cid, LocalDateTime time, int count) {
        log.debug("MessageDaoImpl#getBefore - cid={}, time={}, count={}", cid, time, count);
        LambdaQueryWrapper<MessagePO> queryWrapper = new LambdaQueryWrapper<>();
        // 查询指定频道在某个时间点之前的若干条消息，按时间倒序
        queryWrapper.eq(MessagePO::getCid, cid)
                .lt(MessagePO::getSendTime, time)
                .orderByDesc(MessagePO::getSendTime)
                .last("LIMIT " + count);
        List<MessagePO> records = messageMapper.selectList(queryWrapper);
        CPMessage[] result = records.stream()
                .map(MessagePO::toBo)
                .toArray(CPMessage[]::new);
        log.debug("MessageDaoImpl#getBefore - resultCount={}, cid={}", result.length, cid);
        return result;
    }

    @Override
    public int getAfterCount(long cid, long uid, LocalDateTime time) {
        log.debug("MessageDaoImpl#getAfterCount - cid={}, uid={}, time={}", cid, uid, time);
        LambdaQueryWrapper<MessagePO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MessagePO::getCid, cid)
                .eq(MessagePO::getUid, uid)
                .gt(MessagePO::getSendTime, time);
        int count = messageMapper.selectCount(queryWrapper).intValue();
        log.debug("MessageDaoImpl#getAfterCount - resultCount={}, cid={}, uid={}", count, cid, uid);
        return count;
    }

    @Override
    @CacheEvict(cacheNames = "messageById", key = "#message.id")
    public boolean save(CPMessage message) {
        if (message == null) {
            log.error("MessageDaoImpl#save called with null message");
            return false;
        }
        boolean success = messageMapper.insertOrUpdate(MessagePO.fromBo(message));
        if (success) {
            log.debug("MessageDaoImpl#save success, mid={}, cid={}, uid={}", message.getId(), message.getCid(), message.getUid());
        } else {
            log.warn("MessageDaoImpl#save failed, mid={}, cid={}, uid={}", message.getId(), message.getCid(), message.getUid());
        }
        return success;
    }

    @Override
    @CacheEvict(cacheNames = "messageById", key = "#message.id")
    public boolean delete(CPMessage message) {
        if (message == null) {
            log.error("MessageDaoImpl#delete called with null message");
            return false;
        }
        boolean success = messageMapper.deleteById(message.getId()) != 0;
        if (success) {
            log.debug("MessageDaoImpl#delete success, mid={}", message.getId());
        } else {
            log.warn("MessageDaoImpl#delete failed, mid={}", message.getId());
        }
        return success;
    }
}
