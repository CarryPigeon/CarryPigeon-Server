package team.carrypigeon.backend.api.bo.domain.message;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 聊天消息的BO结构
 * */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class CPMessageBO {
    ///  消息唯一id
    private long id;
    ///  发送者id
    private long sendUserId;
    ///  接受者id，指向通信结构的唯一id
    private long toId;
    ///  消息域，标识为核心类型还是插件类型
    private CPMessageDomain domain;
    ///  消息数据
    private CPMessageData data;
    ///  消息发送时间，以入库时间为准
    private long sendTime;

    public JsonNode toJsonData(ObjectMapper objectMapper){
        return objectMapper.createObjectNode()
                .put("id",id)
                .put("send_user_id",sendUserId)
                .put("to_id",toId)
                .put("domain",domain.toDomain())
                .put("type",data.getType())
                .putPOJO("data",data.getData())
                .put("send_time",sendTime);
    }
}