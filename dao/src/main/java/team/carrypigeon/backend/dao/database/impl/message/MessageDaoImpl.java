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

import java.util.List;
import java.util.Optional;

/**
 * {@link ChannelMessageDao} 的数据库实现（MyBatis-Plus + Spring Cache）。
 * <p>
 * 注意：消息内容在表中以 JSON 字符串存储，{@link MessagePO} 会在 BO/PO 转换时负责序列化与反序列化。
 */
@Slf4j
@Service
public class MessageDaoImpl implements ChannelMessageDao {

    private final MessageMapper messageMapper;

    /**
     * 创建消息 DAO 实现（由 Spring 注入 {@link MessageMapper}）。
     */
    public MessageDaoImpl(MessageMapper messageMapper) {
        this.messageMapper = messageMapper;
    }

    /**
     * 按主键查询数据。
     *
     * @param id 消息 ID
     * @return 匹配的消息对象；不存在时返回 {@code null}
     */
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

    /**
     * 按游标向前分页查询消息。
     *
     * @param cid 频道 ID
     * @param cursorMid 游标消息 ID（不含该条）
     * @param count 最大返回条数
     * @return 按消息 ID 倒序排列的消息数组
     */
    @Override
    public CPMessage[] listBefore(long cid, long cursorMid, int count) {
        long safeCursor = cursorMid <= 0 ? Long.MAX_VALUE : cursorMid;
        log.debug("MessageDaoImpl#listBefore - cid={}, cursorMid={}, count={}", cid, safeCursor, count);
        LambdaQueryWrapper<MessagePO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MessagePO::getCid, cid)
                .lt(MessagePO::getId, safeCursor)
                .orderByDesc(MessagePO::getId)
                .last("LIMIT " + count);
        List<MessagePO> records = messageMapper.selectList(queryWrapper);
        CPMessage[] result = records.stream()
                .map(MessagePO::toBo)
                .toArray(CPMessage[]::new);
        log.debug("MessageDaoImpl#listBefore - resultCount={}, cid={}", result.length, cid);
        return result;
    }

    /**
     * 统计游标之后的消息数量。
     *
     * @param cid 频道 ID
     * @param startMid 起始消息 ID（不含该条）
     * @return 游标之后的消息总数
     */
    @Override
    public int countAfter(long cid, long startMid) {
        long safeStart = Math.max(0L, startMid);
        log.debug("MessageDaoImpl#countAfter - cid={}, startMid={}", cid, safeStart);
        LambdaQueryWrapper<MessagePO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MessagePO::getCid, cid)
                .gt(MessagePO::getId, safeStart);
        int count = messageMapper.selectCount(queryWrapper).intValue();
        log.debug("MessageDaoImpl#countAfter - resultCount={}, cid={}", count, cid);
        return count;
    }

    /**
     * 保存消息数据。
     *
     * @param message 待保存的消息实体
     * @return {@code true} 表示写库成功
     */
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

    /**
     * 删除消息数据。
     *
     * @param message 待删除的消息实体
     * @return {@code true} 表示删除成功
     */
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
