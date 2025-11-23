package team.carrypigeon.backend.chat.domain.controller.netty.user.login.token.logout;

import team.carrypigeon.backend.api.chat.domain.controller.CPControllerDefaultResult;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;

/**
 * 用户登出的控制器<br/>
 * 登出请求url:/core/user/login/token/logout<br/>
 * 请求参数:{@link CPUserTokenLogoutVO} <br/>
 * 登出成功返回参数：无返回值<br/>
 * @author midreamsheep
 *  */
@CPControllerTag(
        path = "/core/user/login/token/logout", voClazz = CPUserTokenLogoutVO.class, resultClazz = CPControllerDefaultResult.class
)
public class CPUserTokenLogoutController {}
