package team.carrypigeon.backend.chat.domain.controller.netty.channel.member.delete;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPChannelDeleteMemberVO {
    private long cid;
    private long uid;
}
