package team.carrypigeon.backend.chat.domain.controller.netty.channel.message.list;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 拉取消息列表的请求
 * @author midreamsheep
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPMessageListVO {
    private long cid;
    private long startTime;
    private int count;
}