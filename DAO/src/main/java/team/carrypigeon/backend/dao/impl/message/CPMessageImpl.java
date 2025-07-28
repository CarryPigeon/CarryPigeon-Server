package team.carrypigeon.backend.dao.impl.message;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import team.carrypigeon.backend.api.bo.domain.message.CPMessageData;
import team.carrypigeon.backend.api.bo.domain.message.CPMessageDomain;
import team.carrypigeon.backend.api.bo.domain.message.CPMessageDomainEnum;
import team.carrypigeon.backend.api.dao.message.CPMessageDAO;
import team.carrypigeon.backend.api.bo.domain.message.CPMessageBO;
import team.carrypigeon.backend.dao.mapper.message.MessageMapper;
import team.carrypigeon.backend.dao.mapper.message.MessagePO;

import java.time.ZoneId;

@Component
public class CPMessageImpl implements CPMessageDAO {

    private final ObjectMapper mapper;

    private final MessageMapper messageMapper;

    public CPMessageImpl(ObjectMapper mapper, MessageMapper messageMapper) {
        this.mapper = mapper;
        this.messageMapper = messageMapper;
    }


    @SneakyThrows
    @Override
    public boolean saveMessage(CPMessageBO message) {
        MessagePO messagePO = new MessagePO();
        messagePO.setId(message.getId());
        messagePO.setToId(message.getToId());
        messagePO.setSendUserId(message.getSendUserId());
        messagePO.setType(message.getData().getType());
        messagePO.setData(mapper.writeValueAsString(message.getData().getData()));
        switch(message.getDomain().getType()) {
            case CORE:
                messagePO.setDomain("core");
                break;
            case PLUGINS:
                messagePO.setDomain("plugins:"+message.getDomain().getPluginName());
                break;
        }
        messageMapper.insert(messagePO);
        return true;
    }

    @Override
    public boolean deleteMessage(long id, long userId) {
        Wrapper<MessagePO> wrapper = new QueryWrapper<MessagePO>()
                .eq("id", id)
                .eq("send_user_id", userId);
        return messageMapper.delete(wrapper)>0;
    }

    @SneakyThrows
    @Override
    public CPMessageBO getMessage(long id) {
        // 从数据库获取数据
        MessagePO messagePO = messageMapper.selectById(id);
        // 非空处理
        if (messagePO == null) return null;
        // 封装数据
        CPMessageBO message = new CPMessageBO();
        message.setId(messagePO.getId())
                .setToId(messagePO.getToId())
                .setSendUserId(messagePO.getSendUserId())
                .setSendTime(messagePO.getSendTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());

        CPMessageData cpMessageData = new CPMessageData()
                .setType(messagePO.getType())
                .setData(mapper.readValue(messagePO.getData(), JsonNode.class));
        message.setData(cpMessageData);
        // 封装domain
        CPMessageDomain cpMessageDomain = new CPMessageDomain();
        if (messagePO.getDomain().equals("core")) {
            cpMessageDomain.setType(CPMessageDomainEnum.CORE);
        } else {
            cpMessageDomain.setType(CPMessageDomainEnum.PLUGINS);
            cpMessageDomain.setPluginName(messagePO.getDomain().split(":")[1]);
        }
        message.setDomain(cpMessageDomain);
        // 返回数据
        return message;
    }

    @Override
    public Long[] getMessageFromTime(long channelId, long fromTime, int count) {
        long fromTimeInSeconds = fromTime / 1000;
        QueryWrapper<MessagePO> queryWrapper = new QueryWrapper<MessagePO>()
                .apply("send_time < FROM_UNIXTIME({0})", fromTimeInSeconds)
                .eq("to_id", channelId)
                .orderByDesc("send_time")
                .last("LIMIT " + count);
        MessagePO[] messagePOS = messageMapper.selectList(queryWrapper).toArray(new MessagePO[0]);
        if (messagePOS.length > 0) {
            Long[] ids = new Long[messagePOS.length];
            for (int i = 0; i < messagePOS.length; i++) {
                ids[i] = messagePOS[i].getId();
            }
            return ids;
        }
        return new Long[0];
    }
}
