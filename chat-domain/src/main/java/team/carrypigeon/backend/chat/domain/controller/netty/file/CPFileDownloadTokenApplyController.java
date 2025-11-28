package team.carrypigeon.backend.chat.domain.controller.netty.file;

import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;

/**
 * 申请下载文件一次性 token 的控制器。<br/>
 * 路由：/core/file/download/token/apply
 */
@CPControllerTag(
        path = "/core/file/download/token/apply",
        voClazz = CPFileDownloadTokenApplyVO.class,
        resultClazz = CPFileDownloadTokenApplyResult.class
)
public class CPFileDownloadTokenApplyController {
}

