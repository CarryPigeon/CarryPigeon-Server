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
 * ????????? token ????<br/>
 * bind: key=UPLOAD|DOWNLOAD<br/>
 * ???<br/>
 *  - SessionId:Long?? UserLoginChecker ???<br/>
 *  - FileInfo_Id:String????????UPLOAD ??????<br/>
 * ???<br/>
 *  - FileToken:String
 */
@Slf4j
@AllArgsConstructor
@LiteflowComponent("CPFileTokenCreator")
public class CPFileTokenCreatorNode extends CPNodeComponent {

    private final FileTokenService fileTokenService;

    @Override
    protected void process(CPSession session, DefaultContext context) throws Exception {
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
        // ?? 5 ?????
        long ttlSec = 300L;
        String token = fileTokenService.createToken(uid, op, fileId, ttlSec);
        context.setData(CPNodeValueKeyBasicConstants.FILE_TOKEN, token);
        log.info("create file token, op={}, uid={}, fileId={}", op, uid, fileId);
    }
}
