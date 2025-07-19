package team.carrypigeon.backend.dao.impl.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import team.carrypigeon.backend.api.dao.message.CPMessageDAO;
import team.carrypigeon.backend.api.bo.domain.message.CPMessageBO;
import team.carrypigeon.backend.dao.mapper.message.MessageMapper;
import team.carrypigeon.backend.dao.mapper.message.MessagePO;

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
        return false;
    }

    @Override
    public CPMessageBO getMessage(long id) {
        return null;
    }
}
