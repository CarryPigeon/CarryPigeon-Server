package team.carrypigeon.backend.dao.database.impl.message;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.api.bo.domain.message.CPMessage;
import team.carrypigeon.backend.api.dao.database.message.ChannelMessageDao;
import team.carrypigeon.backend.dao.database.mapper.message.MessageMapper;
import team.carrypigeon.backend.dao.database.mapper.message.MessagePO;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class MessageDaoImpl implements ChannelMessageDao {

    private final MessageMapper messageMapper;

    public MessageDaoImpl(MessageMapper messageMapper) {
        this.messageMapper = messageMapper;
    }

    @Override
    public CPMessage getById(long id) {
        return Optional.ofNullable(messageMapper.selectById(id)).map(MessagePO::toBo).orElse(null);
    }

    @Override
    public CPMessage[] getBefore(long cid, LocalDateTime time, int count) {
        LambdaQueryWrapper<MessagePO> queryWrapper = new LambdaQueryWrapper<>();
        // 查询指定时间之前的消息，按时间倒序排列
        queryWrapper.eq(MessagePO::getCid, cid)
                .lt(MessagePO::getSendTime, time)
                .orderByDesc(MessagePO::getSendTime)
                .last("LIMIT " + count);
        return messageMapper.selectList(queryWrapper).stream()
                .map(MessagePO::toBo)
                .toArray(CPMessage[]::new);
    }

    @Override
    public int getAfterCount(long cid, long uid, LocalDateTime time) {
        LambdaQueryWrapper<MessagePO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MessagePO::getCid, cid)
                .eq(MessagePO::getUid, uid)
                .gt(MessagePO::getSendTime, time);
        return messageMapper.selectCount(queryWrapper).intValue();
    }

    @Override
    public boolean save(CPMessage message) {
        return messageMapper.insertOrUpdate(MessagePO.fromBo(message));
    }

    @Override
    public boolean delete(CPMessage message) {
        return messageMapper.deleteById(message.getId())!=0;
    }
}
