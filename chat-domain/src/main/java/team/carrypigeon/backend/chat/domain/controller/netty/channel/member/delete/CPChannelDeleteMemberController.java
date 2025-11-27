package team.carrypigeon.backend.chat.domain.controller.netty.channel.member.delete;

import team.carrypigeon.backend.api.chat.domain.controller.CPControllerDefaultResult;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;

/**
 * 删除频道成员的控制器<br/>
 * url: /core/channel/member/delete<br/>
 * 请求参数:{@link CPChannelDeleteMemberVO}<br/>
 * 成功响应参数: 默认成功响应<br/>
 * @author midreamsheep
 */
@CPControllerTag(
        path = "/core/channel/member/delete",
        voClazz = CPChannelDeleteMemberVO.class,
        resultClazz = CPControllerDefaultResult.class
)
public class CPChannelDeleteMemberController {
}

