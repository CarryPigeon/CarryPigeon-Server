package team.carrypigeon.backend.chat.domain.controller.netty.service.email.send;

import com.fasterxml.jackson.databind.ObjectMapper;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerAbstract;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.dao.cache.CPCache;
import team.carrypigeon.backend.api.service.email.CPEmailService;
import team.carrypigeon.backend.chat.domain.permission.login.LoginPermission;

import java.util.Map;

/**
 * 发送邮件<br/>
 * url: /core/service/email/send<br/>
 * 请求参数:{@link CPServiceSendEmailVO}<br/>
 * 返回参数:空<br/>
 * @author midreamsheep
 */
@CPControllerTag("/core/service/email/send")
public class CPServiceSendEmailController extends CPControllerAbstract<CPServiceSendEmailVO> {

    private final CPCache cache;
    private final CPEmailService emailService;

    public CPServiceSendEmailController(ObjectMapper objectMapper, CPCache cache, CPEmailService emailService) {
        super(objectMapper, CPServiceSendEmailVO.class);
        this.cache = cache;
        this.emailService = emailService;
    }

    @Override
    @LoginPermission
    protected CPResponse check(CPSession session, CPServiceSendEmailVO data, Map<String, Object> context) {
        // 判断数据有效性
        if (!data.getEmail().matches("^[a-zA-Z0-9_+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+$")) {
            return CPResponse.ERROR_RESPONSE.copy().setTextData("email error");
        }
        // TODO 检查ip是否频繁发送邮件
        return null;
    }

    @Override
    protected CPResponse process0(CPSession session, CPServiceSendEmailVO data, Map<String, Object> context) {
        // 生成6为随机验证码
        String code = String.valueOf((int)(Math.random() * 1000000));
        cache.set(data.getEmail() + ":code", code, 300);
        // 发送邮件
        // TODO 优化邮件信息
        emailService.sendEmail(data.getEmail(), "验证码", "验证码为:" + code);
        return CPResponse.SUCCESS_RESPONSE.copy();
    }
}
