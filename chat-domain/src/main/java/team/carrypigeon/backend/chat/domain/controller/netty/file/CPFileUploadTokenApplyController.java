package team.carrypigeon.backend.chat.domain.controller.netty.file;

import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;

/**
 * 申请上传文件一次性 token 的控制器。<br/>
 * 路由：/core/file/upload/token/apply
 */
@CPControllerTag(
        path = "/core/file/upload/token/apply",
        voClazz = CPFileUploadTokenApplyVO.class,
        resultClazz = CPFileUploadTokenApplyResult.class
)
public class CPFileUploadTokenApplyController {
}

