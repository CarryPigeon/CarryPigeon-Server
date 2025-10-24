package team.carrypigeon.backend.chat.domain.controller.netty.channel.ban.delete;

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
public class CPChannelDeleteBanVO {
    private long cid;
    private long uid;
}
