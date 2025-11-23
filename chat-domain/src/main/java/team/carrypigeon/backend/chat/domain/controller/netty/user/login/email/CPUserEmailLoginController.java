package team.carrypigeon.backend.chat.domain.controller.netty.user.login.email;

import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;

/**
 * 用户注册的控制器<br/>
 * 请求url:/core/user/login/email<br/>
 * 请求参数:{@link CPUserEmailLoginVO}<br/>
 * 成功响应参数: {@link CPUserEmailLoginResult}<br/>
 * */
@CPControllerTag(
    path = "/core/user/login/email", voClazz = CPUserEmailLoginVO.class, resultClazz = CPUserEmailLoginResult.class
)
public class CPUserEmailLoginController {}