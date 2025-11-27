package team.carrypigeon.backend.chat.domain.controller.netty.channel.message.unread.get;

import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;

/**
 * 获取未读消息数量的控制器<br/>
 * url: /core/channel/message/unread/get<br/>
 * 请求参数:{@link CPMessageGetUnreadVO}<br/>
 * 返回参数:{@link CPMessageGetUnreadResult}<br/>
 * @author midreamsheep
 */
@CPControllerTag(
        path = "/core/channel/message/unread/get",
        voClazz = CPMessageGetUnreadVO.class,
        resultClazz = CPMessageGetUnreadResult.class
)
public class CPMessageGetUnreadController {
}

