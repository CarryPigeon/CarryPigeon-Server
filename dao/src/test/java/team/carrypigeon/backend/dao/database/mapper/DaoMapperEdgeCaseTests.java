package team.carrypigeon.backend.dao.database.mapper;

import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.dao.database.mapper.channel.read.ChannelReadStatePO;
import team.carrypigeon.backend.dao.database.mapper.message.MessagePO;

import static org.junit.jupiter.api.Assertions.*;

class DaoMapperEdgeCaseTests {

    @Test
    void channelReadStatePo_fromBoNull_shouldReturnNull() {
        assertNull(ChannelReadStatePO.fromBo(null));
    }

    @Test
    void messagePo_toBo_invalidJson_shouldThrow() {
        MessagePO po = new MessagePO().setData("{");
        assertThrows(Exception.class, po::toBo);
    }

    @Test
    void messagePo_fromBo_nullMessage_shouldThrow() {
        assertThrows(Exception.class, () -> MessagePO.fromBo(null));
    }
}
