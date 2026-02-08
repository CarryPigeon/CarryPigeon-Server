package team.carrypigeon.backend.chat.domain.cmp.biz.message;

import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;

import com.fasterxml.jackson.databind.JsonNode;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.flow.CheckResult;
import team.carrypigeon.backend.api.chat.domain.message.CPMessageData;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeBindKeys;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelMemberKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeMessageKeys;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyExtraConstants;
import team.carrypigeon.backend.chat.domain.service.catalog.ApiDomainContractService;
import team.carrypigeon.backend.chat.domain.service.message.CPMessageParserService;
import team.carrypigeon.backend.chat.domain.service.message.type.CPRawMessageData;

import java.util.List;
import java.util.Map;

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

    private static final String BIND_TYPE_KEY = CPNodeBindKeys.TYPE;

    private final CPMessageParserService cpMessageParserService;
    private final ApiDomainContractService domainContractService;

    @Override
    protected void process(CPFlowContext context) throws Exception {
        String type = getBindData(BIND_TYPE_KEY, String.class);
        boolean soft = "soft".equalsIgnoreCase(type);

        String domain = requireContext(context, CPNodeMessageKeys.MESSAGE_INFO_DOMAIN);
        String domainVersion = context.get(CPNodeMessageKeys.MESSAGE_INFO_DOMAIN_VERSION);
        if (domainVersion == null || domainVersion.isBlank()) {
            domainVersion = "1.0.0";
        }
        JsonNode data = requireContext(context, CPNodeMessageKeys.MESSAGE_INFO_DATA);

        CPMessageData messageData;
        if (cpMessageParserService.supports(domain)) {
            messageData = cpMessageParserService.parse(domain, data);
            if (messageData == null) {
                if (soft) {
                    context.set(CPFlowKeys.CHECK_RESULT, new CheckResult(false, "message type error"));
                    log.info("CPMessageParse soft fail: schema invalid, domain={}", domain);
                    return;
                }
                log.warn("CPMessageParse hard fail: schema invalid, domain={}", domain);
                throw new CPProblemException(CPProblem.of(422, "schema_invalid", "schema invalid",
                        Map.of("field_errors", List.of(
                                Map.of("field", "data", "reason", "invalid", "message", "schema invalid")
                        ))));
            }
        } else if (domainContractService.supports(domain, domainVersion)) {
            // Plugin domain: validate with JSON schema then wrap raw data
            try {
                domainContractService.validateOrThrow(domain, domainVersion, data);
            } catch (CPProblemException e) {
                if (soft) {
                    context.set(CPFlowKeys.CHECK_RESULT, new CheckResult(false, "message type error"));
                    log.info("CPMessageParse soft fail: schema invalid, domain={}, version={}", domain, domainVersion);
                    return;
                }
                throw e;
            }
            messageData = new CPRawMessageData(domain, data);
        } else {
            if (soft) {
                context.set(CPFlowKeys.CHECK_RESULT, new CheckResult(false, "message type error"));
                log.info("CPMessageParse soft fail: unsupported domain, domain={}, version={}", domain, domainVersion);
                return;
            }
            log.warn("CPMessageParse hard fail: unsupported domain, domain={}, version={}", domain, domainVersion);
            throw new CPProblemException(CPProblem.of(422, "validation_failed", "validation failed",
                    Map.of("field_errors", List.of(
                            Map.of("field", "domain", "reason", "unsupported", "message", "unsupported message domain")
                    ))));
        }

        context.set(CPNodeValueKeyExtraConstants.MESSAGE_DATA, messageData);
        if (soft) {
            context.set(CPFlowKeys.CHECK_RESULT, new CheckResult(true, null));
        }
        Long cid = context.get(CPNodeChannelKeys.CHANNEL_INFO_ID);
        Long uid = context.get(CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO_UID);
        log.debug("CPMessageParse success, domain={}, cid={}, uid={}", domain, cid, uid);
    }
}
