package team.carrypigeon.backend.chat.domain.controller.netty.channel.ban.list;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 获取频道的封禁列表响应值
 * @author midreamsheep
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPChannelListBanResult {
    private int count;
    private CPChannelListBanResultItem[] bans;
}
