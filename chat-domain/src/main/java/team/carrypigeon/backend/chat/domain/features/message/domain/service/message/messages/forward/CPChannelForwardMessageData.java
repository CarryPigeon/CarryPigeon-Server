package team.carrypigeon.backend.chat.domain.features.message.domain.service.message.messages.forward;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 转发消息的数据
 * */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPChannelForwardMessageData {
    /**转发自哪个频道*/
    private long sourceChannelId;
    /**消息id列表*/
    private List<Long> messageIds;
}
