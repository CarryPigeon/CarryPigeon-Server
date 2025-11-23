package team.carrypigeon.backend.chat.domain.controller.netty.channel.create;

import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;

/**
 * 创建通道的接口<br/>
 * 请求url:/core/channel/create<br/>
 * 请求参数:{@link CPChannelCreateVO}<br/>
 * 成功返回参数:{@link CPChannelCreateResult}<br/>
 * @author midreamsheep
 * */
@CPControllerTag(
    path = "/core/channel/create", voClazz = CPChannelCreateVO.class
)
public class CPChannelCreateController{}
