package team.carrypigeon.backend.chat.domain.controller.netty.user.profile.update;

import team.carrypigeon.backend.api.chat.domain.controller.CPControllerDefaultResult;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;

/**
 * 更新用户信息控制器<br/>
 * 请求url:/core/user/profile/update<br/>
 * 请求参数:{@link CPUserUpdateProfileVO}<br/>
 * 响应参数:无<br/>
 * */
@CPControllerTag(
        path = "/core/user/profile/update",
        voClazz = CPUserUpdateProfileVO.class,
        resultClazz = CPControllerDefaultResult.class
)
public class CPUserUpdateProfileController{}