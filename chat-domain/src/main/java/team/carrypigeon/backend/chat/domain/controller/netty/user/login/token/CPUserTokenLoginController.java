package team.carrypigeon.backend.chat.domain.controller.netty.user.login.token;

import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;

/**
 * 用户注册的控制器<br/>
 * 请求url:/core/user/login/token<br/>
 * 请求参数:{@link CPUserTokenLoginVO}<br/>
 * 成功响应参数: {@link CPUserTokenLoginResult}<br/>
 * */
@CPControllerTag(
        path = "/core/user/login/token", voClazz = CPUserTokenLoginVO.class, resultClazz = CPUserTokenLoginResult.class
)
public class CPUserTokenLoginController{}