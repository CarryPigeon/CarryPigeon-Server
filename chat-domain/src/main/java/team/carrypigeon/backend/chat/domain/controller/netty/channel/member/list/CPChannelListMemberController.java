package team.carrypigeon.backend.chat.domain.controller.netty.channel.member.list;

import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;

/**
 * 获取频道成员列表的控制器<br/>
 * 访问url:/core/channel/member/list<br/>
 * 访问参数:{@link CPChannelListMemberVO}<br/>
 * 访问返回:{@link CPChannelListMemberResult}<br/>
 * @author midreamsheep
 */
@CPControllerTag(
        path = "/core/channel/member/list",
        voClazz = CPChannelListMemberVO.class,
        resultClazz = CPChannelListMemberResult.class
)
public class CPChannelListMemberController {
}

