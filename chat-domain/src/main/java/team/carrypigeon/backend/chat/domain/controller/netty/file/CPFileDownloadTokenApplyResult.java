package team.carrypigeon.backend.chat.domain.controller.netty.file;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerResult;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyBasicConstants;

/**
 * 下载文件 token 申请的返回结果。
 */
public class CPFileDownloadTokenApplyResult implements CPControllerResult {

    @Override
    public void process(CPSession session, DefaultContext context, ObjectMapper objectMapper) {
        String token = context.getData(CPNodeValueKeyBasicConstants.FILE_TOKEN);
        if (token == null || token.isEmpty()) {
            argsError(context);
            return;
        }
        Result result = new Result(token);
        context.setData(CPNodeValueKeyBasicConstants.RESPONSE,
                CPResponse.SUCCESS_RESPONSE.copy().setData(objectMapper.valueToTree(result)));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class Result {
        private String token;
    }
}
