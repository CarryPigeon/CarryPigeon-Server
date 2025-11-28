package team.carrypigeon.backend.chat.domain.controller.netty.service.email.send;

import team.carrypigeon.backend.api.chat.domain.controller.CPControllerDefaultResult;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;

/**
 * 发送邮箱验证码的控制器<br/>
 * 请求url:/core/service/email/send<br/>
 * 请求参数:{@link CPServiceSendEmailVO}<br/>
 * 成功响应参数: 默认成功响应<br/>
 * @author midreamsheep
 */
@CPControllerTag(
        path = "/core/service/email/send",
        voClazz = CPServiceSendEmailVO.class
)
public class CPServiceSendEmailController {
}

