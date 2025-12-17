package team.carrypigeon.backend.chat.domain.controller.netty.service.email.send;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerVO;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyExtraConstants;

/**
 * 发送邮件验证码的请求参数
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPServiceSendEmailVO implements CPControllerVO {

    private String email;

    @Override
    public boolean insertData(CPFlowContext context) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        context.setData(CPNodeValueKeyExtraConstants.EMAIL, email);
        return true;
    }
}
