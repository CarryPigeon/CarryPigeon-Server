package team.carrypigeon.backend.chat.domain.controller.netty.channel.application.create;

import team.carrypigeon.backend.api.chat.domain.controller.CPControllerDefaultResult;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;

/**
 * 创建频道申请<br/>
 * url: /core/channel/application/create <br/>
 * 参数: {@link CPChannelCreateApplicationVO} <br/>
 * 返回参数: 无
 * @author midreamsheep
 * */
@CPControllerTag(
        path = "/core/channel/application/create",
        voClazz = CPChannelCreateApplicationVO.class,
        resultClazz = CPControllerDefaultResult.class
)
public class CPChannelCreateApplicationController{}