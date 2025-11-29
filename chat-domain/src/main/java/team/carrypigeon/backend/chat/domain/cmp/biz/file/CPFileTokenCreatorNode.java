package team.carrypigeon.backend.chat.domain.cmp.biz.file;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.chat.domain.controller.CPNodeComponent;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyBasicConstants;
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
    public void process(CPSession session, DefaultContext context) throws Exception {
        String op = getBindData("key", String.class);
        if (op == null || op.isEmpty()) {
            argsError(context);
            return;
        }
        Long uid = context.getData(CPNodeValueKeyBasicConstants.SESSION_ID);
        if (uid == null) {
            argsError(context);
            return;
        }
        String fileId = context.getData(CPNodeValueKeyBasicConstants.FILE_INFO_ID);
        // 默认 5 分钟有效期
        long ttlSec = 300L;
        String token = fileTokenService.createToken(uid, op, fileId, ttlSec);
        context.setData(CPNodeValueKeyBasicConstants.FILE_TOKEN, token);
        log.info("create file token, op={}, uid={}, fileId={}", op, uid, fileId);
    }
}
