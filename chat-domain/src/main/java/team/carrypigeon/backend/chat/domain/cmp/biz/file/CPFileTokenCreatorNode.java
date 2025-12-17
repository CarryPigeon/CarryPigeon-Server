package team.carrypigeon.backend.chat.domain.cmp.biz.file;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeBindKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeCommonKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeFileKeys;
import team.carrypigeon.backend.chat.domain.service.file.FileTokenService;

/**
 * 生成文件上传 / 下载操作的一次性 token 的节点。<br/>
 * bind: key=UPLOAD|DOWNLOAD<br/>
 * 输入：<br/>
 *  - SessionId:Long  当前登录用户 id，由登录校验节点写入<br/>
 *  - FileInfo_Id:String  需要进行 UPLOAD/DOWNLOAD 的文件标识<br/>
 * 输出：<br/>
 *  - FileToken:String  一次性文件操作 token
 */
@Slf4j
@AllArgsConstructor
@LiteflowComponent("CPFileTokenCreator")
public class CPFileTokenCreatorNode extends CPNodeComponent {

    private final FileTokenService fileTokenService;

    @Override
    public void process(CPSession session, CPFlowContext context) throws Exception {
        String op = requireBind(context, CPNodeBindKeys.KEY, String.class);
        Long uid = requireContext(context, CPNodeCommonKeys.SESSION_ID, Long.class);
        String fileId = context.getData(CPNodeFileKeys.FILE_INFO_ID);
        // 默认 5 分钟有效期
        long ttlSec = 300L;
        String token = fileTokenService.createToken(uid, op, fileId, ttlSec);
        context.setData(CPNodeFileKeys.FILE_TOKEN, token);
        log.info("create file token, op={}, uid={}, fileId={}", op, uid, fileId);
    }
}
