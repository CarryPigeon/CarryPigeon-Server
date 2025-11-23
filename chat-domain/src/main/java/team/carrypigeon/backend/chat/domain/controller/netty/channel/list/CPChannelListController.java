package team.carrypigeon.backend.chat.domain.controller.netty.channel.list;

import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;

/**
 * 拉取通道的接口<br/>
 * 请求url：/core/channel/list<br/>
 * 请求参数：空<br/>
 * 请求响应：{@link CPChannelListResult}
 * @author midreamsheep
 * */
@CPControllerTag(
        path = "/core/channel/list", voClazz = CPChannelListVO.class, resultClazz = CPChannelListResult.class
)
public class CPChannelListController {}
