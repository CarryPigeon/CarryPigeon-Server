package team.carrypigeon.backend.chat.domain.controller.netty.channel.admin.create;

import team.carrypigeon.backend.api.chat.domain.controller.CPControllerDefaultResult;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;

/**
 * 创建管理员<br/>
 * 请求url:/core/channel/admin/create<br/>
 * 请求参数:{@link CPChannelCreateAdminVO}<br/>
 * 成功返回参数:无<br/>
 * @author midreamsheep
 * */
@CPControllerTag(
        path = "/core/channel/admin/create", voClazz = CPChannelCreateAdminVO.class, resultClazz = CPControllerDefaultResult.class
)
public class CPChannelCreateAdminController  {}
