package team.carrypigeon.backend.chat.domain.controller.netty.channel.application.list;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 获取通道申请列表的参数
 * @author midreamsheep
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPChannelListApplicationVO {
    private long cid;
    private int page;
    private int pageSize;
}
