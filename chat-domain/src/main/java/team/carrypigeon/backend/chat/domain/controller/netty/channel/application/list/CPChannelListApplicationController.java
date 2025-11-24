package team.carrypigeon.backend.chat.domain.controller.netty.channel.application.list;

import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;

/**
 * 获取通道申请列表的接口<br/>
 * 访问url:/core/channel/application/list<br/>
 * 访问参数:{@link CPChannelListApplicationVO}<br/>
 * 访问返回:{@link CPChannelListApplicationResult}<br/>
 * @author midreamsheep
 */
@CPControllerTag(
        path = "/core/channel/application/list",
        voClazz = CPChannelListApplicationVO.class,
        resultClazz = CPChannelListApplicationResult.class
)
public class CPChannelListApplicationController{}