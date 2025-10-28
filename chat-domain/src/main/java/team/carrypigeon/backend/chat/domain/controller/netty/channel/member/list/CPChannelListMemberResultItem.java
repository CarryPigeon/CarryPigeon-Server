package team.carrypigeon.backend.chat.domain.controller.netty.channel.member.list;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class CPChannelListMemberResultItem {
    private long uid;
    private String name;
    private int authority;
    private long joinTime;
}
