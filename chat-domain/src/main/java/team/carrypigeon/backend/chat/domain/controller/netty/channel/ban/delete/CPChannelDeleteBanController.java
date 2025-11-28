package team.carrypigeon.backend.chat.domain.controller.netty.channel.ban.delete;

import team.carrypigeon.backend.api.chat.domain.controller.CPControllerDefaultResult;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;

/**
 * 删除频道封禁的控制器。<br/>
 * 请求 url: /core/channel/ban/delete<br/>
 * 请求参数: {@link CPChannelDeleteBanVO}<br/>
 * 返回参数: 默认成功响应
 */
@CPControllerTag(
        path = "/core/channel/ban/delete",
        voClazz = CPChannelDeleteBanVO.class,
        resultClazz = CPControllerDefaultResult.class
)
public class CPChannelDeleteBanController {
}

