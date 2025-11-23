package team.carrypigeon.backend.chat.domain.controller.netty.user.register;

import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;

/**
 * 用户注册的控制器<br/>
 * 请求url:/core/user/register<br/>
 * 请求参数:{@link CPUserRegisterVO}<br/>
 * 成功响应参数: {@link CPUserRegisterResult}<br/>
 * */
@CPControllerTag(
        path = "/core/user/register",
        voClazz = CPUserRegisterVO.class
)
public class CPUserRegisterController {}