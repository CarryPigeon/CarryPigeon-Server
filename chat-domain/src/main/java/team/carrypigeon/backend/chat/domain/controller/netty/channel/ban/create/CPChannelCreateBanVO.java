package team.carrypigeon.backend.chat.domain.controller.netty.channel.ban.create;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建频道的封禁列表的参数
 * @author midreamsheep
 * */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPChannelCreateBanVO {
    private long cid;
    private long uid;
    private int duration;
}
