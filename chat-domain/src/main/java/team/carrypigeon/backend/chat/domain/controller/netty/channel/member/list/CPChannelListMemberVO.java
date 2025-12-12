package team.carrypigeon.backend.chat.domain.controller.netty.channel.member.list;

import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerVO;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPChannelListMemberVO implements CPControllerVO {
    private long cid;

    @Override
    public boolean insertData(DefaultContext context) {
        context.setData(CPNodeChannelKeys.CHANNEL_INFO_ID, cid);
        return true;
    }
}
