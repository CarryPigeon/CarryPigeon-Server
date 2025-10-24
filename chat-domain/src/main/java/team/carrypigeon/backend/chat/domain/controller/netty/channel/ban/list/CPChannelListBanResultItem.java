package team.carrypigeon.backend.chat.domain.controller.netty.channel.ban.list;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class CPChannelListBanResultItem {
    private long uid;
    private long aid;
    private long banTime;
    private int duration;
}
