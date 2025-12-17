package team.carrypigeon.backend.chat.domain.controller.netty.channel.member.get;

import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;

/**
 * 获取单个频道成员信息的控制器<br/>
 * 请求url:/core/channel/member/get<br/>
 * 请求参数:{@link CPChannelGetMemberVO}<br/>
 * 响应参数:{@link CPChannelGetMemberResult}<br/>
 * @author midreamsheep
 */
@CPControllerTag(
        path = "/core/channel/member/get",
        voClazz = CPChannelGetMemberVO.class,
        resultClazz = CPChannelGetMemberResult.class
)
public class CPChannelGetMemberController {
}

