package team.carrypigeon.backend.chat.domain.controller.netty.channel.list;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class CPChannelListResultItem {
    private long cid;
    private String name;
    private long owner;
    private long avatar;
    private String brief;
}
