package team.carrypigeon.backend.chat.domain.controller.netty.channel.message.get;

import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;

/**
 * 获取单条频道消息的控制器<br/>
 * 请求url:/core/channel/message/get<br/>
 * 请求参数:{@link CPMessageGetVO}<br/>
 * 响应参数:{@link CPMessageGetResult}<br/>
 * @author midreamsheep
 */
@CPControllerTag(
        path = "/core/channel/message/get",
        voClazz = CPMessageGetVO.class,
        resultClazz = CPMessageGetResult.class
)
public class CPMessageGetController {
}

