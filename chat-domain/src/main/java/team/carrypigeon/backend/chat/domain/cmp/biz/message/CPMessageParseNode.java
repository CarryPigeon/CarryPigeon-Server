package team.carrypigeon.backend.chat.domain.cmp.biz.message;

import com.fasterxml.jackson.databind.JsonNode;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.chat.domain.controller.CPNodeComponent;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.api.chat.domain.message.CPMessageData;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyBasicConstants;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyExtraConstants;
import team.carrypigeon.backend.chat.domain.cmp.info.CheckResult;
import team.carrypigeon.backend.chat.domain.service.message.CPMessageParserService;

/**
 * 将原始消息域 + 数据解析为业务层 {@link CPMessageData}。<br/>
 * 输入：<br/>
 *  - MessageInfo_Domain:String  消息域，例如 Core:Text<br/>
 *  - MessageInfo_Data:JsonNode  消息原始 JSON 数据<br/>
 * 输出：<br/>
 *  - MessageData:{@link CPMessageData}<br/>
 * 行为：<br/>
 *  - bind 参数 type=hard|soft，soft 表示解析失败只写入 {@link CheckResult} 而不中断流程，<br/>
 *    hard（默认）表示解析失败会返回错误响应并终止流程。
 */
@Slf4j
@AllArgsConstructor
@LiteflowComponent("CPMessageParse")
public class CPMessageParseNode extends CPNodeComponent {

    private static final String BIND_TYPE_KEY = "type";

    private final CPMessageParserService cpMessageParserService;

    @Override
    public void process(CPSession session, DefaultContext context) throws Exception {
        String type = getBindData(BIND_TYPE_KEY, String.class);
        boolean soft = "soft".equalsIgnoreCase(type);

        String domain = context.getData(CPNodeValueKeyBasicConstants.MESSAGE_INFO_DOMAIN);
        JsonNode data = context.getData(CPNodeValueKeyBasicConstants.MESSAGE_INFO_DATA);
        if (domain == null || data == null) {
            log.error("CPMessageParse args error: domain or data is null");
            argsError(context);
            return;
        }
        CPMessageData messageData = cpMessageParserService.parse(domain, data);
        if (messageData == null) {
            if (soft) {
                context.setData(CPNodeValueKeyBasicConstants.CHECK_RESULT,
                        new CheckResult(false, "message type error"));
                log.info("CPMessageParse soft fail: unsupported or invalid type, domain={}", domain);
                return;
            }
            log.warn("CPMessageParse hard fail: unsupported or invalid type, domain={}", domain);
            context.setData(CPNodeValueKeyBasicConstants.RESPONSE,
                    CPResponse.ERROR_RESPONSE.copy().setTextData("message type error"));
            throw new CPReturnException();
        }
        context.setData(CPNodeValueKeyExtraConstants.MESSAGE_DATA, messageData);
        if (soft) {
            context.setData(CPNodeValueKeyBasicConstants.CHECK_RESULT, new CheckResult(true, null));
        }
        Long cid = context.getData(CPNodeValueKeyBasicConstants.CHANNEL_INFO_ID);
        Long uid = context.getData(CPNodeValueKeyBasicConstants.CHANNEL_MEMBER_INFO_UID);
        log.debug("CPMessageParse success, domain={}, cid={}, uid={}", domain, cid, uid);
    }
}
