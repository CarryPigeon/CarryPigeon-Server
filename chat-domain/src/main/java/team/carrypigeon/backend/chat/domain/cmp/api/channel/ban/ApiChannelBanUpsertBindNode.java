package team.carrypigeon.backend.chat.domain.cmp.api.channel.ban;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemException;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemReason;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelBanKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyExtraConstants;
import team.carrypigeon.backend.chat.domain.controller.web.api.dto.ChannelBanTargetRequest;

import java.util.List;
import java.util.Map;

/**
 * 频道禁言新增/更新请求绑定节点。
 * <p>
 * 解析 `PUT /api/channels/{cid}/bans/{uid}` 请求并写入禁言参数。
 */
@Slf4j
@LiteflowComponent("ApiChannelBanUpsertBind")
public class ApiChannelBanUpsertBindNode extends CPNodeComponent {

    /**
     * 解析并绑定禁言新增/更新参数。
     */
    @Override
    protected void process(CPFlowContext context) {
        Object reqObj = context.get(CPFlowKeys.REQUEST);
        if (!(reqObj instanceof ChannelBanTargetRequest req) || req.body() == null) {
            throw new CPProblemException(CPProblem.of(CPProblemReason.VALIDATION_FAILED, "validation failed"));
        }
        long cid = parseId(req.cid(), "cid");
        long uid = parseId(req.uid(), "uid");
        long until = req.body().until() == null ? 0L : req.body().until();
        if (until <= System.currentTimeMillis()) {
            throw new CPProblemException(CPProblem.of(CPProblemReason.VALIDATION_FAILED, "validation failed",
                    Map.of("field_errors", List.of(
                            Map.of("field", "until", "reason", "invalid", "message", "must be in the future")
                    ))));
        }
        context.set(CPNodeChannelKeys.CHANNEL_INFO_ID, cid);
        context.set(CPNodeChannelBanKeys.CHANNEL_BAN_TARGET_UID, uid);
        context.set(CPNodeChannelBanKeys.CHANNEL_BAN_UNTIL_TIME, until);
        context.set(CPNodeChannelBanKeys.CHANNEL_BAN_REASON, req.body().reason());
        context.set(CPNodeValueKeyExtraConstants.TARGET_MEMBER_UID, uid);
        log.debug("ApiChannelBanUpsertBind success, cid={}, targetUid={}, until={}", cid, uid, until);
    }

    /**
     * 解析字符串形式的 ID。
     */
    private long parseId(String str, String field) {
        try {
            return Long.parseLong(str);
        } catch (Exception e) {
            throw new CPProblemException(CPProblem.of(CPProblemReason.VALIDATION_FAILED, "validation failed",
                    Map.of("field_errors", List.of(
                            Map.of("field", field, "reason", "invalid", "message", "invalid id")
                    ))));
        }
    }
}
