package team.carrypigeon.backend.chat.domain.controller.netty.channel.ban.create;

import team.carrypigeon.backend.api.chat.domain.controller.CPControllerDefaultResult;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;

/**
 * 创建频道封禁的控制器<br/>
 * 请求url:/core/channel/ban/create<br/>
 * 请求参数:{@link CPChannelCreateBanVO}<br/>
 * 返回参数: 默认成功响应<br/>
 * @author midreamsheep
 */
@CPControllerTag(
        path = "/core/channel/ban/create",
        voClazz = CPChannelCreateBanVO.class
)
public class CPChannelCreateBanController {
}

