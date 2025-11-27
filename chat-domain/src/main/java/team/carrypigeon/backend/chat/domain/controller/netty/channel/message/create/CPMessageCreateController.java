package team.carrypigeon.backend.chat.domain.controller.netty.channel.message.create;

import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;

/**
 * 发送消息的控制器<br/>
 * url:/core/channel/message/create<br/>
 * 请求参数:{@link CPMessageCreateVO}<br/>
 * 返回参数:{@link CPMessageCreateResult}<br/>
 * @author midreamsheep
 */
@CPControllerTag(
        path = "/core/channel/message/create",
        voClazz = CPMessageCreateVO.class,
        resultClazz = CPMessageCreateResult.class
)
public class CPMessageCreateController {
}

