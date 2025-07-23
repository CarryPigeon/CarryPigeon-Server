package team.carrypigeon.backend.api.bo.domain.message;

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
    private LocalDateTime sendTime;
}