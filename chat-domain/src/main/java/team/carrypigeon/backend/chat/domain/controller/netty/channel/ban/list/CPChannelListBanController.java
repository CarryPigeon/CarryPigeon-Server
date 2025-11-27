package team.carrypigeon.backend.chat.domain.controller.netty.channel.ban.list;

import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;

/**
 * 获取频道封禁列表的控制器<br/>
 * 请求url:/core/channel/ban/list<br/>
 * 请求参数:{@link CPChannelListBanVO}<br/>
 * 返回参数:{@link CPChannelListBanResult}<br/>
 * @author midreamsheep
 */
@CPControllerTag(
        path = "/core/channel/ban/list",
        voClazz = CPChannelListBanVO.class,
        resultClazz = CPChannelListBanResult.class
)
public class CPChannelListBanController {
}

