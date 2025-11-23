package team.carrypigeon.backend.chat.domain.controller.netty.user.profile.get;

import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;

/**
 * 获取用户信息控制器<br/>
 * 请求url:/core/user/profile/get<br/>
 * 请求参数:{@link CPUserGetProfileVO}<br/>
 * 响应参数:{@link CPUserGetProfileResult}<br/>
 * */
@CPControllerTag(
        path = "/core/user/profile/get",
        voClazz = CPUserGetProfileVO.class,
        resultClazz = CPUserGetProfileResult.class
)
public class CPUserGetProfileController{}