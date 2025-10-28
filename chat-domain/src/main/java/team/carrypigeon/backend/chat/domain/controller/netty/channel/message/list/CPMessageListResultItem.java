package team.carrypigeon.backend.chat.domain.controller.netty.channel.message.list;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 获取消息列表结果项
 * @author midreamsheep
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class CPMessageListResultItem {
    private long mid;
    private String domain;
    private long uid;
    private long cid;
    private JsonNode data;
    private long sendTime;
}