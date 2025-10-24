package team.carrypigeon.backend.chat.domain.controller.netty.channel.ban.list;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 获取频道的封禁列表的参数
 * @author midreamsheep
 * */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPChannelListBanVO {
    private long cid;
}
