package team.carrypigeon.backend.chat.domain.cmp.api.message;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemException;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeApiKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeMessageKeys;
import team.carrypigeon.backend.chat.domain.controller.web.api.dto.MessageCreateRequest;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;

import java.util.List;
import java.util.Map;

/**
 * 消息创建请求绑定节点（HTTP：{@code POST /api/channels/{cid}/messages}）。
 *
 * <p>职责：将 HTTP DTO {@link MessageCreateRequest} 解析/校验后写入 LiteFlow 上下文，
 * 以便后续节点（权限校验、限流、schema 校验、落库、结果映射）复用统一的 key。
 *
 * <p>输入：{@link CPFlowKeys#REQUEST} = {@link MessageCreateRequest}
 *
 * <p>输出（写入上下文）：
 * <ul>
 *   <li>{@link CPNodeChannelKeys#CHANNEL_INFO_ID}: channel id</li>
 *   <li>{@link CPNodeMessageKeys#MESSAGE_INFO_DOMAIN}: message domain</li>
 *   <li>{@link CPNodeMessageKeys#MESSAGE_INFO_DOMAIN_VERSION}: message domain version</li>
 *   <li>{@link CPNodeMessageKeys#MESSAGE_INFO_DATA}: message payload (JsonNode)</li>
 * </ul>
 */
@Slf4j
@LiteflowComponent("ApiMessageCreateBind")
public class ApiMessageCreateBindNode extends CPNodeComponent {

    /** 默认 domain_version。 */
    private static final String DEFAULT_DOMAIN_VERSION = "1.0.0";

    /**
     * 解析并绑定消息创建请求。
     *
     * <p>依赖上下文：{@link CPFlowKeys#REQUEST}（类型必须为 {@link MessageCreateRequest}）。
     *
     * <p>输出：写入频道/域/数据等上下文字段（见类注释）。
     *
     * <p>失败语义：参数非法时抛出 {@link CPProblemException}（HTTP 422，{@code validation_failed}）。
     */
    @Override
    protected void process(CPFlowContext context) {
        Object reqObj = context.get(CPFlowKeys.REQUEST);
        if (!(reqObj instanceof MessageCreateRequest req)) {
            throw new CPProblemException(CPProblem.of(422, "validation_failed", "validation failed"));
        }
        long cid = parseId(req.cid(), "cid");
        if (req.domain() == null || req.domain().isBlank()) {
            throw new CPProblemException(CPProblem.of(422, "validation_failed", "validation failed",
                    Map.of("field_errors", List.of(
                            Map.of("field", "domain", "reason", "invalid", "message", "domain is required")
                    ))));
        }
        if (req.data() == null) {
            throw new CPProblemException(CPProblem.of(422, "validation_failed", "validation failed",
                    Map.of("field_errors", List.of(
                            Map.of("field", "data", "reason", "invalid", "message", "data is required")
                    ))));
        }
        context.set(CPNodeChannelKeys.CHANNEL_INFO_ID, cid);
        context.set(CPNodeMessageKeys.MESSAGE_INFO_DOMAIN, req.domain());
        context.set(CPNodeMessageKeys.MESSAGE_INFO_DOMAIN_VERSION,
                (req.domainVersion() == null || req.domainVersion().isBlank()) ? DEFAULT_DOMAIN_VERSION : req.domainVersion());
        long replyToMid = parseOptionalId(req.replyToMid(), "reply_to_mid");
        context.set(CPNodeMessageKeys.MESSAGE_INFO_REPLY_TO_MID, replyToMid);
        context.set(CPNodeMessageKeys.MESSAGE_INFO_DATA, req.data());
        if (req.idempotencyKey() != null && !req.idempotencyKey().isBlank()) {
            context.set(CPNodeApiKeys.IDEMPOTENCY_KEY, req.idempotencyKey().trim());
        }
        log.debug("消息创建绑定：完成：cid={}, domain={}", cid, req.domain());
    }

    /**
     * 解析必填的雪花 ID（十进制字符串）。
     *
     * @param str 原始字符串
     * @param field 字段名（用于 field_errors）
     * @return 解析后的 long
     */
    private long parseId(String str, String field) {
        try {
            return Long.parseLong(str);
        } catch (Exception e) {
            throw new CPProblemException(CPProblem.of(422, "validation_failed", "validation failed",
                    Map.of("field_errors", List.of(
                            Map.of("field", field, "reason", "invalid", "message", "invalid id")
                    ))));
        }
    }

    /**
     * 解析可选雪花 ID：空值视为 0；非法则按 422 返回。
     *
     * @param str 原始字符串（允许为空）
     * @param field 字段名（用于 field_errors）
     * @return 解析后的 long；未提供时为 0
     */
    private long parseOptionalId(String str, String field) {
        if (str == null || str.isBlank()) {
            return 0L;
        }
        long v = parseId(str, field);
        if (v < 0) {
            throw new CPProblemException(CPProblem.of(422, "validation_failed", "validation failed",
                    Map.of("field_errors", List.of(
                            Map.of("field", field, "reason", "invalid", "message", "invalid id")
                    ))));
        }
        return v;
    }
}
