package team.carrypigeon.backend.chat.domain.cmp.api.channel;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemException;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemReason;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyExtraConstants;
import team.carrypigeon.backend.chat.domain.controller.web.api.dto.ChannelMemberTargetRequest;

import java.util.List;
import java.util.Map;

/**
 * 频道成员目标绑定节点。
 * <p>
 * 用于需要 `cid + targetUid` 的接口，解析后写入上下文目标成员键。
 */
@Slf4j
@LiteflowComponent("ApiChannelMemberTargetBind")
public class ApiChannelMemberTargetBindNode extends CPNodeComponent {

    /**
     * 解析并绑定目标成员参数。
     */
    @Override
    protected void process(CPFlowContext context) {
        Object reqObj = context.get(CPFlowKeys.REQUEST);
        if (!(reqObj instanceof ChannelMemberTargetRequest req)) {
            throw new CPProblemException(CPProblem.of(CPProblemReason.VALIDATION_FAILED, "validation failed"));
        }
        long cid = parseId(req.cid(), "cid");
        long uid = parseId(req.uid(), "uid");
        context.set(CPNodeChannelKeys.CHANNEL_INFO_ID, cid);
        context.set(CPNodeValueKeyExtraConstants.TARGET_MEMBER_UID, uid);
        log.debug("ApiChannelMemberTargetBind success, cid={}, targetUid={}", cid, uid);
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
