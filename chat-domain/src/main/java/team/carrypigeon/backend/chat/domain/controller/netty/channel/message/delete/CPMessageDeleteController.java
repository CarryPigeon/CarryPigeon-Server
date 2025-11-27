package team.carrypigeon.backend.chat.domain.controller.netty.channel.message.delete;

import team.carrypigeon.backend.api.chat.domain.controller.CPControllerDefaultResult;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;

/**
 * 删除消息的控制器<br/>
 * url:/core/channel/message/delete<br/>
 * 请求参数:{@link CPMessageDeleteVO}<br/>
 * 返回参数: 默认成功响应<br/>
 * @author midreamsheep
 */
@CPControllerTag(
        path = "/core/channel/message/delete",
        voClazz = CPMessageDeleteVO.class,
        resultClazz = CPControllerDefaultResult.class
)
public class CPMessageDeleteController {
}

