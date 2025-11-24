package team.carrypigeon.backend.chat.domain.controller.netty.channel.application.process;

import team.carrypigeon.backend.api.chat.domain.controller.CPControllerDefaultResult;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;

/**
 * 处理申请加入的接口<br/>
 * url: /core/channel/application/process <br/>
 * 请求参数:{@link CPChannelProcessApplicationVO}<br/>
 * 返回参数:无
 * @author midreamsheep
 * */
@CPControllerTag(
        path = "/core/channel/application/process",
        voClazz = CPChannelProcessApplicationVO.class,
        resultClazz = CPControllerDefaultResult.class
)
public class CPChannelProcessApplicationController{}