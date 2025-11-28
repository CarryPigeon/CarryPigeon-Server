package team.carrypigeon.backend.chat.domain.controller.netty.file;

import com.yomahub.liteflow.slot.DefaultContext;
import lombok.Data;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerVO;

/**
 * 请求上传文件 token 的参数。
 * 当前上传只需要用户身份，不需要额外参数，因此该 VO 为空实现。
 */
@Data
public class CPFileUploadTokenApplyVO implements CPControllerVO {

    @Override
    public boolean insertData(DefaultContext context) {
        // no extra args, SessionId will be filled by UserLoginChecker
        return true;
    }
}

