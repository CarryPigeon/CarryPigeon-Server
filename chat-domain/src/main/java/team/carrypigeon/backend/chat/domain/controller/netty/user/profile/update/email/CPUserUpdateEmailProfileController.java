package team.carrypigeon.backend.chat.domain.controller.netty.user.profile.update.email;

import team.carrypigeon.backend.api.chat.domain.controller.CPControllerDefaultResult;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;

/**
 * 更新用户邮箱信息<br/>
 * 请求url: /core/user/profile/update/email<br/>
 * 请求参数: {@link CPUserUpdateEmailProfileVO}<br/>
 * */
@CPControllerTag(
        path = "/core/user/profile/update/email",
        voClazz = CPUserUpdateEmailProfileVO.class,
        resultClazz = CPControllerDefaultResult.class
)
public class CPUserUpdateEmailProfileController{}