package team.carrypigeon.backend.chat.domain.controller.netty.user.account.email;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.domain.CPChannel;
import team.carrypigeon.backend.api.chat.domain.controller.CPController;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;
import team.carrypigeon.backend.api.connection.vo.CPResponse;
import team.carrypigeon.backend.chat.domain.manager.email.CPEmailCode;
import team.carrypigeon.backend.chat.domain.manager.email.CPEmailCodeManager;

import java.time.LocalDateTime;

/**
 * 邮箱认证申请,一个邮箱验证码是一次敏感操作申请，只能使用一次
 * */
@Slf4j
@CPControllerTag("/core/account/email")
public class CPEmailController implements CPController {

    private final ObjectMapper objectMapper;

    private final CPEmailCodeManager emailCodeManager;

    public CPEmailController(ObjectMapper objectMapper, CPEmailCodeManager emailCodeManager) {
        this.objectMapper = objectMapper;
        this.emailCodeManager = emailCodeManager;
    }

    @SneakyThrows
    @Override
    public CPResponse process(JsonNode data, CPChannel channel) {
        CPEmailVO cpEmailVO = objectMapper.treeToValue(data, CPEmailVO.class);
        // 生成随机6位code
        CPEmailCode code = new CPEmailCode( (int)(Math.random() * 1000000), LocalDateTime.now());
        // TODO 邮件发送
        log.info("email:{},code:{}", cpEmailVO.getEmail(), code.getCode());
        emailCodeManager.addCode(cpEmailVO.getEmail(), code);
        return CPResponse.SUCCESS_RESPONSE.copy();
    }
}
