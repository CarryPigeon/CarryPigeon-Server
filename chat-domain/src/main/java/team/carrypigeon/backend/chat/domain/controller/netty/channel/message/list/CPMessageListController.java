package team.carrypigeon.backend.chat.domain.controller.netty.channel.message.list;

import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;

/**
 * 拉取频道消息列表的控制器<br/>
 * url: /core/channel/message/list<br/>
 * 请求参数:{@link CPMessageListVO}<br/>
 * 返回参数:{@link CPMessageListResult}<br/>
 * @author midreamsheep
 */
@CPControllerTag(
        path = "/core/channel/message/list",
        voClazz = CPMessageListVO.class,
        resultClazz = CPMessageListResult.class
)
public class CPMessageListController {
}

